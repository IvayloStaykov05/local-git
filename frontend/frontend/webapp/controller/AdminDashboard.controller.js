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

                if (
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403 ||
                    oUsersResponse.status === 401 || oUsersResponse.status === 403
                ) {
                    this._handleUnauthorized();
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

            if (oResponse.status === 401 || oResponse.status === 403) {
                this._handleUnauthorized();
                return [];
            }

            if (!oResponse.ok) {
                throw new Error(sText || "Version history loading failed");
            }

            var aVersions = sText ? JSON.parse(sText) : [];

            return (Array.isArray(aVersions) ? aVersions : []).map(function (oVersion) {
                return {
                    versionId: oVersion.versionId,
                    versionNumber: oVersion.versionNumber,
                    status: oVersion.status || "",
                    createdBy: oVersion.createdBy || "",
                    approvedBy: oVersion.approvedBy || "",
                    rejectedBy: oVersion.rejectedBy || "",
                    createdAt: oVersion.createdAt || null
                };
            });
        },

        _confirmDeleteWholeDocument: function (iDocumentId, sTitle) {
            var that = this;

            MessageBox.confirm(
                "Are you sure you want to delete document \"" + (sTitle || "") + "\"?",
                {
                    title: "Delete document",
                    actions: [MessageBox.Action.DELETE, MessageBox.Action.CANCEL],
                    emphasizedAction: MessageBox.Action.DELETE,
                    onClose: async function (sAction) {
                        if (sAction !== MessageBox.Action.DELETE) {
                            return;
                        }

                        await that._deleteWholeDocument(iDocumentId);
                    }
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

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Document deletion failed");
                }

                MessageToast.show("Document deleted successfully");

                this._removeDocumentFromTable(iDocumentId);
                this.onCloseDeleteDocumentDialog();
            } catch (oError) {
                MessageBox.error("Cannot delete document: " + oError.message);
            }
        },

        _removeDocumentFromTable: function (iDocumentId) {
            var oModel = this.getView().getModel("admin");
            var aDocuments = oModel.getProperty("/documents") || [];

            aDocuments = aDocuments.filter(function (oDocument) {
                return oDocument.id !== iDocumentId;
            });

            oModel.setProperty("/documents", aDocuments);
        },

        _updateDocumentVersionsCount: function (iDocumentId, iNewCount) {
            var oModel = this.getView().getModel("admin");
            var aDocuments = oModel.getProperty("/documents") || [];

            aDocuments = aDocuments.map(function (oDocument) {
                if (oDocument.id === iDocumentId) {
                    return Object.assign({}, oDocument, {
                        versionsCount: iNewCount
                    });
                }
                return oDocument;
            });

            oModel.setProperty("/documents", aDocuments);
        },

        onDeleteVersionPress: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDeleteDialogData = this.getView().getModel("admin").getProperty("/deleteDialog");
            var that = this;

            MessageBox.confirm(
                "Are you sure you want to delete version " + oVersion.versionNumber + " of document \"" + (oDeleteDialogData.title || "") + "\"?",
                {
                    title: "Delete version",
                    actions: [MessageBox.Action.DELETE, MessageBox.Action.CANCEL],
                    emphasizedAction: MessageBox.Action.DELETE,
                    onClose: async function (sAction) {
                        if (sAction !== MessageBox.Action.DELETE) {
                            return;
                        }

                        await that._deleteVersion(oVersion.versionId);
                    }
                }
            );
        },

        _deleteVersion: async function (iVersionId) {
            var oModel = this.getView().getModel("admin");
            var oDeleteDialogData = oModel.getProperty("/deleteDialog");
            var iDocumentId = oDeleteDialogData.documentId;

            try {
                const oResponse = await fetch("http://localhost:8080/api/admin/documentVersions/" + iVersionId, {
                    method: "DELETE",
                    headers: this._getAuthHeaders()
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Version deletion failed");
                }

                var aVersions = oDeleteDialogData.versions || [];
                aVersions = aVersions.filter(function (oVersion) {
                    return oVersion.versionId !== iVersionId;
                });

                var iNewCount = aVersions.length;

                oModel.setProperty("/deleteDialog/versions", aVersions);
                oModel.setProperty("/deleteDialog/versionsCount", iNewCount);

                this._updateDocumentVersionsCount(iDocumentId, iNewCount);

                MessageToast.show("Version deleted successfully");
            } catch (oError) {
                MessageBox.error("Cannot delete version: " + oError.message);
            }
        },

        onDeleteWholeDocumentFromDialog: function () {
            var oDeleteDialogData = this.getView().getModel("admin").getProperty("/deleteDialog");

            if (!oDeleteDialogData || !oDeleteDialogData.documentId) {
                return;
            }

            this._confirmDeleteWholeDocument(
                oDeleteDialogData.documentId,
                oDeleteDialogData.title
            );
        },

        onCloseDeleteDocumentDialog: function () {
            var oDialog = this.byId("adminDeleteDocumentDialog");
            if (oDialog) {
                oDialog.close();
            }

            this.getView().getModel("admin").setProperty("/deleteDialog", {
                documentId: null,
                title: "",
                versionsCount: 0,
                versions: []
            });
        },

        onOpenUserDetails: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oUser = oContext.getObject();

            try {
                const oResponse = await fetch("http://localhost:8080/api/users/" + oUser.id, {
                    method: "GET",
                    headers: this._getAuthHeaders()
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot load user profile");
                }

                var oProfile = sText ? JSON.parse(sText) : {};
                var oMappedUser = this._mapUser(oProfile);

                this.getView().getModel("admin").setProperty("/selectedUser", oMappedUser);

                if (!this._pAdminUserDialog) {
                    this._pAdminUserDialog = Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.AdminUserDetailsDialog",
                        controller: this
                    }).then(function (oDialog) {
                        this.getView().addDependent(oDialog);
                        return oDialog;
                    }.bind(this));
                }

                var oDialog = await this._pAdminUserDialog;
                oDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot load user details: " + oError.message);
            }
        },

        onCloseAdminUserDetailsDialog: function () {
            var oDialog = this.byId("adminUserDetailsDialog");
            if (oDialog) {
                oDialog.close();
            }
        },

        onToggleUserActive: async function () {
            var oModel = this.getView().getModel("admin");
            var oSelectedUser = oModel.getProperty("/selectedUser");
            var oCurrentUser = oModel.getProperty("/currentUser");

            if (!oSelectedUser || !oSelectedUser.id) {
                return;
            }

            if (oCurrentUser && oCurrentUser.id === oSelectedUser.id) {
                MessageBox.warning("You cannot deactivate your own admin account.");
                return;
            }

            var bWillDeactivate = !!oSelectedUser.active;
            var sUrl = bWillDeactivate
                ? "http://localhost:8080/api/admin/users/deactivate"
                : "http://localhost:8080/api/admin/users/activate";

            try {
                const oResponse = await fetch(sUrl, {
                    method: "PATCH",
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        userId: oSelectedUser.id,
                        reason: "Status changed by admin from admin dashboard."
                    })
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "User status update failed");
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
                MessageBox.error("Cannot open create admin dialog: " + oError.message);
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
                "createAdminUsernameInput",
                "createAdminFirstNameInput",
                "createAdminLastNameInput",
                "createAdminEmailInput",
                "createAdminPasswordInput",
                "createAdminConfirmPasswordInput"
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
            var oFirstNameInput = this._getCreateAdminField("createAdminFirstNameInput");
            var oLastNameInput = this._getCreateAdminField("createAdminLastNameInput");
            var oEmailInput = this._getCreateAdminField("createAdminEmailInput");
            var oPasswordInput = this._getCreateAdminField("createAdminPasswordInput");
            var oConfirmPasswordInput = this._getCreateAdminField("createAdminConfirmPasswordInput");

            var sUsername = oUsernameInput.getValue().trim();
            var sFirstName = oFirstNameInput.getValue().trim();
            var sLastName = oLastNameInput.getValue().trim();
            var sEmail = oEmailInput.getValue().trim();
            var sPassword = oPasswordInput.getValue();
            var sConfirmPassword = oConfirmPasswordInput.getValue();

            if (!sUsername || !sFirstName || !sLastName || !sEmail || !sPassword || !sConfirmPassword) {
                MessageBox.error("Please fill all fields.");
                return;
            }

            if (sPassword !== sConfirmPassword) {
                oConfirmPasswordInput.setValueState("Error");
                oConfirmPasswordInput.setValueStateText("Passwords do not match");
                MessageBox.error("Passwords do not match.");
                return;
            }

            oConfirmPasswordInput.setValueState("None");
            oConfirmPasswordInput.setValueStateText("");

            try {
                const oResponse = await fetch("http://localhost:8080/api/admin-invitations/create-profile", {
                    method: "POST",
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        adminUsername: sUsername,
                        firstName: sFirstName,
                        lastName: sLastName,
                        adminEmail: sEmail,
                        adminPassword: sPassword
                    })
                });

                const sText = await oResponse.text();

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                if (!oResponse.ok) {
                    throw new Error(sText || "Admin creation failed");
                }

                var oResult = sText ? JSON.parse(sText) : {};
                MessageToast.show(oResult.message || "Admin created successfully");

                this.onCloseCreateAdminDialog();
                this._clearCreateAdminForm();
                this._loadAdminData(this.getView().getModel("admin").getProperty("/search"));
            } catch (oError) {
                MessageBox.error("Cannot create admin: " + oError.message);
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