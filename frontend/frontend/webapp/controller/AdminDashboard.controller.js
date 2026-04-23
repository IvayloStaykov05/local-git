sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageBox",
    "sap/m/MessageToast",
    "sap/ui/core/Fragment"
], function (Controller, JSONModel, MessageBox, MessageToast, Fragment) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.AdminDashboard", {

        onInit: function () {
            var sToken = localStorage.getItem("token");
            var oStoredUser = {};

            try {
                oStoredUser = JSON.parse(localStorage.getItem("user") || "{}");
            } catch (e) {
                oStoredUser = {};
            }

            if (!sToken) {
                this.getOwnerComponent().getRouter().navTo("RouteLogin");
                return;
            }

            if (oStoredUser.systemRole !== "ADMIN") {
                MessageBox.error("You do not have access to this page.");
                this.getOwnerComponent().getRouter().navTo("RouteDashboard");
                return;
            }

            var oModel = new JSONModel({
                currentUser: oStoredUser,
                search: "",
                documents: [],
                users: [],
                selectedUser: {
                    id: null,
                    username: "",
                    firstName: "",
                    lastName: "",
                    fullName: "",
                    email: "",
                    systemRole: "",
                    myInfo: "",
                    active: true
                },
                deleteDialog: {
                    documentId: null,
                    title: "",
                    versionsCount: 0,
                    versions: []
                }
            });

            this.getView().setModel(oModel, "admin");
            this._loadAdminData("");
        },

        _getAuthHeaders: function () {
            return {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            };
        },

        _handleUnauthorized: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        _handleForbidden: function (sMessage, bCloseCreateAdminDialog) {
            if (bCloseCreateAdminDialog) {
                this.onCloseCreateAdminDialog();
                this._clearCreateAdminForm();
            }

            MessageBox.error(sMessage || "You do not have permission to perform this action.");
        },

        _mapUser: function (oUser) {
            var sFirstName = oUser.firstName || "";
            var sLastName = oUser.lastName || "";
            var sUsername = oUser.username || "";
            var sFullName = (sFirstName + " " + sLastName).trim() || sUsername;

            return {
                id: oUser.id,
                username: sUsername,
                firstName: sFirstName,
                lastName: sLastName,
                fullName: sFullName,
                email: oUser.email || "",
                systemRole: oUser.systemRole || "",
                myInfo: oUser.myInfo || "",
                active: typeof oUser.active === "boolean" ? oUser.active : true
            };
        },

        _loadAdminData: async function (sSearch) {
            var oModel = this.getView().getModel("admin");
            var sEncodedSearch = encodeURIComponent((sSearch || "").trim());

            try {
                const [oDocumentsResponse, oUsersResponse] = await Promise.all([
                    fetch("http://localhost:8080/api/admin/documents?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/admin/users/search?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    })
                ]);

                if (oDocumentsResponse.status === 401 || oUsersResponse.status === 401) {
                    this._handleUnauthorized();
                    return;
                }

                if (oDocumentsResponse.status === 403 || oUsersResponse.status === 403) {
                    this._handleForbidden("You do not have permission to load admin data.", false);
                    return;
                }

                const sDocumentsText = await oDocumentsResponse.text();
                const sUsersText = await oUsersResponse.text();

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Cannot load admin documents");
                }

                if (!oUsersResponse.ok) {
                    throw new Error(sUsersText || "Cannot load admin users");
                }

                var aDocuments = sDocumentsText ? JSON.parse(sDocumentsText) : [];
                var aUsers = sUsersText ? JSON.parse(sUsersText) : [];

                oModel.setProperty("/documents", Array.isArray(aDocuments) ? aDocuments : []);
                oModel.setProperty("/users", (Array.isArray(aUsers) ? aUsers : []).map(this._mapUser));
            } catch (oError) {
                MessageBox.error("Admin data loading failed: " + oError.message);
            }
        },

        onSearchAdmin: function (oEvent) {
            var sValue = oEvent.getSource().getValue();
            this.getView().getModel("admin").setProperty("/search", sValue);
            this._loadAdminData(sValue);
        },

        onSearchAdminLiveChange: function (oEvent) {
            var sValue = oEvent.getParameter("value");
            this.getView().getModel("admin").setProperty("/search", sValue);
            this._loadAdminData(sValue);
        },

        onOpenDocument: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oDocument = oContext.getObject();

            this.getOwnerComponent().getRouter().navTo("RouteDocumentDetails", {
                documentId: String(oDocument.id)
            });
        },

        onDeleteDocument: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oDocument = oContext.getObject();

            if ((oDocument.versionsCount || 0) <= 1) {
                this._confirmDeleteWholeDocument(oDocument.id, oDocument.title);
                return;
            }

            try {
                var aVersions = await this._loadDocumentVersionsForDelete(oDocument.id);

                this.getView().getModel("admin").setProperty("/deleteDialog", {
                    documentId: oDocument.id,
                    title: oDocument.title || "",
                    versionsCount: oDocument.versionsCount || aVersions.length,
                    versions: aVersions
                });

                if (!this._pDeleteDocumentDialog) {
                    this._pDeleteDocumentDialog = Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.AdminDeleteDocumentDialog",
                        controller: this
                    }).then(function (oDialog) {
                        this.getView().addDependent(oDialog);
                        return oDialog;
                    }.bind(this));
                }

                var oDialog = await this._pDeleteDocumentDialog;
                oDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot load document versions: " + oError.message);
            }
        },

        _loadDocumentVersionsForDelete: async function (iDocumentId) {
            const oResponse = await fetch("http://localhost:8080/api/documentVersions/history", {
                method: "POST",
                headers: this._getAuthHeaders(),
                body: JSON.stringify({
                    documentId: iDocumentId
                })
            });

            const sText = await oResponse.text();

            if (oResponse.status === 401) {
                this._handleUnauthorized();
                return [];
            }

            if (oResponse.status === 403) {
                this._handleForbidden("You do not have permission to load document versions.", false);
                return [];
            }

            if (!oResponse.ok) {
                throw new Error(sText || "Cannot load versions");
            }

            var aHistory = sText ? JSON.parse(sText) : [];
            return Array.isArray(aHistory) ? aHistory : [];
        },

        onCloseDeleteDocumentDialog: function () {
            var oDialog = this.byId("adminDeleteDocumentDialog");
            if (oDialog) {
                oDialog.close();
            }
        },

        onDeleteSingleVersion: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDeleteDialogData = this.getView().getModel("admin").getProperty("/deleteDialog") || {};
            var iDocumentId = oDeleteDialogData.documentId;
            var sDocumentTitle = oDeleteDialogData.title || "this document";

            try {
                const oResponse = await fetch("http://localhost:8080/api/admin/documentVersions/" + oVersion.id, {
                    method: "DELETE",
                    headers: this._getAuthHeaders()
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401) {
                    this._handleUnauthorized();
                    return;
                }

                if (oResponse.status === 403) {
                    this._handleForbidden("You do not have permission to delete this version.", false);
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot delete version");
                }

                MessageToast.show("Version deleted successfully");

                var aVersions = await this._loadDocumentVersionsForDelete(iDocumentId);
                var iVersionsCount = aVersions.length;

                if (iVersionsCount <= 1) {
                    this.onCloseDeleteDocumentDialog();
                    this._confirmDeleteWholeDocument(iDocumentId, sDocumentTitle);
                    return;
                }

                this.getView().getModel("admin").setProperty("/deleteDialog", {
                    documentId: iDocumentId,
                    title: sDocumentTitle,
                    versionsCount: iVersionsCount,
                    versions: aVersions
                });

                this._loadAdminData(this.getView().getModel("admin").getProperty("/search"));
            } catch (oError) {
                MessageBox.error("Cannot delete version: " + oError.message);
            }
        },

        _confirmDeleteWholeDocument: function (iDocumentId, sTitle) {
            MessageBox.confirm(
                "Are you sure you want to delete the whole document \"" + (sTitle || "") + "\"?",
                {
                    title: "Delete Document",
                    actions: [MessageBox.Action.DELETE, MessageBox.Action.CANCEL],
                    emphasizedAction: MessageBox.Action.DELETE,
                    onClose: function (sAction) {
                        if (sAction === MessageBox.Action.DELETE) {
                            this._deleteWholeDocument(iDocumentId);
                        }
                    }.bind(this)
                }
            );
        },

        _deleteWholeDocument: async function (iDocumentId) {
            try {
                const oResponse = await fetch("http://localhost:8080/api/admin/documents/" + iDocumentId, {
                    method: "DELETE",
                    headers: this._getAuthHeaders()
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401) {
                    this._handleUnauthorized();
                    return;
                }

                if (oResponse.status === 403) {
                    this._handleForbidden("You do not have permission to delete this document.", false);
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot delete document");
                }

                MessageToast.show("Document deleted successfully");
                this.onCloseDeleteDocumentDialog();
                this._loadAdminData(this.getView().getModel("admin").getProperty("/search"));
            } catch (oError) {
                MessageBox.error("Cannot delete document: " + oError.message);
            }
        },

        onOpenUserDetails: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oUser = oContext.getObject();
            var oModel = this.getView().getModel("admin");

            oModel.setProperty("/selectedUser", Object.assign({}, oUser));

            try {
                if (!this._pAdminUserDetailsDialog) {
                    this._pAdminUserDetailsDialog = Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.AdminUserDetailsDialog",
                        controller: this
                    }).then(function (oDialog) {
                        this.getView().addDependent(oDialog);
                        return oDialog;
                    }.bind(this));
                }

                var oDialog = await this._pAdminUserDetailsDialog;
                oDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot open user details: " + oError.message);
            }
        },

        onCloseAdminUserDetailsDialog: function () {
            var oDialog = this.byId("adminUserDetailsDialog");
            if (oDialog) {
                oDialog.close();
            }
        },

       onToggleUserActiveStatus: async function () {
    var oModel = this.getView().getModel("admin");
    var oSelectedUser = oModel.getProperty("/selectedUser");

    if (!oSelectedUser || !oSelectedUser.id) {
        return;
    }

    var bCurrentlyActive = !!oSelectedUser.active;
    var sUrl = bCurrentlyActive
        ? "http://localhost:8080/api/admin/users/deactivate"
        : "http://localhost:8080/api/admin/users/activate";

    var sReason = bCurrentlyActive
        ? "Deactivated by admin"
        : "Activated by admin";

    try {
        const oResponse = await fetch(sUrl, {
            method: "PATCH",
            headers: this._getAuthHeaders(),
            body: JSON.stringify({
                userId: oSelectedUser.id,
                reason: sReason
            })
        });

        const sText = await oResponse.text();

        if (oResponse.status === 401) {
            this._handleUnauthorized();
            return;
        }

        if (oResponse.status === 403) {
            this._handleForbidden("You do not have permission to change user status.", false);
            return;
        }

        if (!oResponse.ok) {
            throw new Error(sText || "Cannot change user status");
        }

        var oResult = sText ? JSON.parse(sText) : {};
        var bNewActive = !!oResult.active;

        oModel.setProperty("/selectedUser/active", bNewActive);

        var aUsers = oModel.getProperty("/users") || [];
        aUsers = aUsers.map(function (oUser) {
            if (oUser.id === oSelectedUser.id) {
                return Object.assign({}, oUser, {
                    active: bNewActive
                });
            }
            return oUser;
        });

        oModel.setProperty("/users", aUsers);

        MessageToast.show(bNewActive ? "User activated successfully" : "User deactivated successfully");
    } catch (oError) {
        MessageBox.error("Cannot change user status: " + oError.message);
    }
},

        onOpenCreateAdminDialog: async function () {
            try {
                if (!this._pCreateAdminDialog) {
                    this._pCreateAdminDialog = Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.CreateAdminDialog",
                        controller: this
                    }).then(function (oDialog) {
                        this.getView().addDependent(oDialog);
                        return oDialog;
                    }.bind(this));
                }

                var oDialog = await this._pCreateAdminDialog;
                this._clearCreateAdminForm();
                oDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot open admin invitation dialog: " + oError.message);
            }
        },

        onCloseCreateAdminDialog: function () {
            var oDialog = this.byId("createAdminDialog");
            if (oDialog) {
                oDialog.close();
            }
        },

        _getCreateAdminField: function (sFieldId) {
            return Fragment.byId(this.getView().getId(), sFieldId);
        },

        _clearCreateAdminForm: function () {
            var aFieldIds = [
                "createAdminUsernameInput"
            ];

            aFieldIds.forEach(function (sFieldId) {
                var oField = Fragment.byId(this.getView().getId(), sFieldId);
                if (oField) {
                    oField.setValue("");
                    if (oField.setValueState) {
                        oField.setValueState("None");
                    }
                    if (oField.setValueStateText) {
                        oField.setValueStateText("");
                    }
                }
            }.bind(this));
        },

        onSubmitCreateAdmin: async function () {
            var oUsernameInput = this._getCreateAdminField("createAdminUsernameInput");
            var sUsername = oUsernameInput.getValue().trim();

            if (!sUsername) {
                oUsernameInput.setValueState("Error");
                oUsernameInput.setValueStateText("Username is required");
                MessageBox.error("Please enter the username of the user you want to invite.");
                return;
            }

            oUsernameInput.setValueState("None");
            oUsernameInput.setValueStateText("");

            try {
                const oResponse = await fetch("http://localhost:8080/api/admin-invitations", {
                    method: "POST",
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        username: sUsername
                    })
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401) {
                    this._handleUnauthorized();
                    return;
                }

                if (oResponse.status === 403) {
                    this._handleForbidden("You do not have permission to send admin invitations.", true);
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Admin invitation failed");
                }

                var oResult = sText ? JSON.parse(sText) : {};
                MessageToast.show(oResult.message || "Admin invitation sent successfully");

                this.onCloseCreateAdminDialog();
                this._clearCreateAdminForm();
                this._loadAdminData(this.getView().getModel("admin").getProperty("/search"));
            } catch (oError) {
                MessageBox.error("Cannot send admin invitation: " + oError.message);
            }
        },

        onOpenNotifications: function () {
            MessageToast.show("Notifications coming soon...");
        },

        onLogout: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            MessageToast.show("Logged out successfully");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        formatDateTime: function (sValue) {
            if (!sValue) {
                return "";
            }

            var oDate = new Date(sValue);

            if (isNaN(oDate.getTime())) {
                return sValue;
            }

            return oDate.toLocaleString();
        },

        formatVersionDeleteEnabled: function (iVersionsCount) {
            return iVersionsCount > 1;
        }

    });
});