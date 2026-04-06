sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/core/Fragment"
], function (Controller, JSONModel, MessageToast, MessageBox, Fragment) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.Dashboard", {

        onInit: function () {
            var sToken = localStorage.getItem("token");

            if (!sToken) {
                this.getOwnerComponent().getRouter().navTo("RouteLogin");
                return;
            }

            var oStoredUser = {};
            try {
                oStoredUser = JSON.parse(localStorage.getItem("user") || "{}");
            } catch (e) {
                oStoredUser = {};
            }

            var oDashboardModel = new JSONModel({
                myInfo: {
                    id: oStoredUser.id || null,
                    username: oStoredUser.username || "",
                    firstName: "",
                    lastName: "",
                    fullName: oStoredUser.username || "",
                    email: oStoredUser.email || "",
                    systemRole: oStoredUser.systemRole || "",
                    myInfo: ""
                },
                myDocuments: [],
                peopleWithWork: [],
                notifications: [],
                search: "",
                searchUsers: [],
                searchDocuments: [],
                showSearchResults: false,
                selectedUser: {
                    id: null,
                    username: "",
                    fullName: "",
                    email: "",
                    systemRole: "",
                    myInfo: ""
                },
                myInfoDialog: {
                    mode: "add",
                    title: "Add Info",
                    value: ""
                }
            });

            this.getView().setModel(oDashboardModel, "dashboard");
            this._loadOwnProfile();
            this._loadDashboardData();
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
            var sEmail = oUser.email || "";
            var sFullName = (sFirstName + " " + sLastName).trim() || sUsername;

            return {
                id: oUser.id,
                username: sUsername,
                firstName: sFirstName,
                lastName: sLastName,
                email: sEmail,
                fullName: sFullName,
                name: sFullName,
                review: sEmail,
                systemRole: oUser.systemRole || "",
                myInfo: oUser.myInfo || "",
                initials: (
                    ((sFirstName.charAt(0) || "") + (sLastName.charAt(0) || "")) ||
                    sUsername.substring(0, 2)
                ).toUpperCase()
            };
        },

        _loadOwnProfile: async function () {
            var oDashboardModel = this.getView().getModel("dashboard");
            var oStoredUser = oDashboardModel.getProperty("/myInfo");

            if (!oStoredUser.id) {
                return;
            }

            try {
                const oResponse = await fetch("http://localhost:8080/api/users/" + oStoredUser.id, {
                    method: "GET",
                    headers: this._getAuthHeaders()
                });

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot load profile");
                }

                var oProfile = sText ? JSON.parse(sText) : null;
                if (!oProfile) {
                    return;
                }

                var oMappedProfile = this._mapUser(oProfile);

                oDashboardModel.setProperty("/myInfo", {
                    id: oMappedProfile.id,
                    username: oMappedProfile.username,
                    firstName: oMappedProfile.firstName,
                    lastName: oMappedProfile.lastName,
                    fullName: oMappedProfile.fullName,
                    email: oMappedProfile.email,
                    systemRole: oMappedProfile.systemRole,
                    myInfo: oMappedProfile.myInfo
                });
            } catch (oError) {
                MessageBox.error("Cannot load your profile: " + oError.message);
            }
        },

        _loadDashboardData: async function () {
            try {
                const [oDocumentsResponse, oSharedUsersResponse] = await Promise.all([
                    fetch("http://localhost:8080/api/documents/my", {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/documentMembers/shared-users", {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    })
                ]);

                if (
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403 ||
                    oSharedUsersResponse.status === 401 || oSharedUsersResponse.status === 403
                ) {
                    this._handleUnauthorized();
                    return;
                }

                const sDocumentsText = await oDocumentsResponse.text();
                const sSharedUsersText = await oSharedUsersResponse.text();

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Documents loading failed");
                }

                if (!oSharedUsersResponse.ok) {
                    throw new Error(sSharedUsersText || "Shared users loading failed");
                }

                var aDocuments = sDocumentsText ? JSON.parse(sDocumentsText) : [];
                var aSharedUsers = sSharedUsersText ? JSON.parse(sSharedUsersText) : [];

                var aMappedUsers = (Array.isArray(aSharedUsers) ? aSharedUsers : []).map(this._mapUser);

                this.getView().getModel("dashboard").setProperty("/myDocuments", Array.isArray(aDocuments) ? aDocuments : []);
                this.getView().getModel("dashboard").setProperty("/peopleWithWork", aMappedUsers);

            } catch (oError) {
                MessageBox.error("Cannot load dashboard data: " + oError.message);
            }
        },

        _runGlobalSearch: async function (sSearch) {
            var sTrimmedSearch = (sSearch || "").trim();
            var oDashboardModel = this.getView().getModel("dashboard");

            if (!sTrimmedSearch) {
                oDashboardModel.setProperty("/searchUsers", []);
                oDashboardModel.setProperty("/searchDocuments", []);
                oDashboardModel.setProperty("/showSearchResults", false);
                return;
            }

            try {
                var sEncodedSearch = encodeURIComponent(sTrimmedSearch);

                const [oUsersResponse, oDocumentsResponse] = await Promise.all([
                    fetch("http://localhost:8080/api/users/search?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/documents/my?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    })
                ]);

                if (
                    oUsersResponse.status === 401 || oUsersResponse.status === 403 ||
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403
                ) {
                    this._handleUnauthorized();
                    return;
                }

                const sUsersText = await oUsersResponse.text();
                const sDocumentsText = await oDocumentsResponse.text();

                if (!oUsersResponse.ok) {
                    throw new Error(sUsersText || "Users search failed");
                }

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Documents search failed");
                }

                var aUsers = sUsersText ? JSON.parse(sUsersText) : [];
                var aDocuments = sDocumentsText ? JSON.parse(sDocumentsText) : [];

                oDashboardModel.setProperty("/searchUsers", (Array.isArray(aUsers) ? aUsers : []).map(this._mapUser));
                oDashboardModel.setProperty("/searchDocuments", Array.isArray(aDocuments) ? aDocuments : []);
                oDashboardModel.setProperty("/showSearchResults", true);

            } catch (oError) {
                MessageBox.error("Cannot search: " + oError.message);
            }
        },

        _openUserDetailsDialog: async function (iUserId) {
            if (!iUserId) {
                return;
            }

            try {
                const oResponse = await fetch("http://localhost:8080/api/users/" + iUserId, {
                    method: "GET",
                    headers: this._getAuthHeaders()
                });

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot load user details");
                }

                var oUser = sText ? JSON.parse(sText) : null;
                if (!oUser) {
                    return;
                }

                var oMappedUser = this._mapUser(oUser);

                this.getView().getModel("dashboard").setProperty("/selectedUser", {
                    id: oMappedUser.id,
                    username: oMappedUser.username,
                    fullName: oMappedUser.fullName,
                    email: oMappedUser.email,
                    systemRole: oMappedUser.systemRole,
                    myInfo: oMappedUser.myInfo
                });

                if (!this._oUserDetailsDialog) {
                    this._oUserDetailsDialog = await Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.UserDetailsDialog",
                        controller: this
                    });

                    this.getView().addDependent(this._oUserDetailsDialog);
                }

                this._oUserDetailsDialog.open();

            } catch (oError) {
                MessageBox.error("Cannot open user details: " + oError.message);
            }
        },

        onCloseUserDetailsDialog: function () {
            if (this._oUserDetailsDialog) {
                this._oUserDetailsDialog.close();
            }
        },

        onOpenMyInfoMenu: async function () {
            var oDashboardModel = this.getView().getModel("dashboard");
            var sCurrentInfo = oDashboardModel.getProperty("/myInfo/myInfo") || "";

            if (sCurrentInfo) {
                oDashboardModel.setProperty("/myInfoDialog/mode", "update");
                oDashboardModel.setProperty("/myInfoDialog/title", "Update Info");
                oDashboardModel.setProperty("/myInfoDialog/value", sCurrentInfo);
            } else {
                oDashboardModel.setProperty("/myInfoDialog/mode", "add");
                oDashboardModel.setProperty("/myInfoDialog/title", "Add Info");
                oDashboardModel.setProperty("/myInfoDialog/value", "");
            }

            await this._openMyInfoDialog();
        },

        _openMyInfoDialog: async function () {
            if (!this._oMyInfoDialog) {
                this._oMyInfoDialog = await Fragment.load({
                    id: this.getView().getId(),
                    name: "com.example.project.frontend.frontend.view.fragments.MyInfoDialog",
                    controller: this
                });

                this.getView().addDependent(this._oMyInfoDialog);
            }

            this._oMyInfoDialog.open();
        },

        onCloseMyInfoDialog: function () {
            if (this._oMyInfoDialog) {
                this._oMyInfoDialog.close();
            }
        },

        onSaveMyInfo: async function () {
            var oDashboardModel = this.getView().getModel("dashboard");
            var sMode = oDashboardModel.getProperty("/myInfoDialog/mode");
            var sValue = (oDashboardModel.getProperty("/myInfoDialog/value") || "").trim();

            if (!sValue) {
                MessageBox.warning("Personal information cannot be empty.");
                return;
            }

            var sUrl = sMode === "update"
                ? "http://localhost:8080/api/users/updateMyInfo"
                : "http://localhost:8080/api/users/addMyInfo";

            var sMethod = sMode === "update" ? "PATCH" : "POST";

            try {
                const oResponse = await fetch(sUrl, {
                    method: sMethod,
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        info: sValue
                    })
                });

                if (oResponse.status === 401 || oResponse.status === 403) {
                    this._handleUnauthorized();
                    return;
                }

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Saving personal information failed");
                }

                this.onCloseMyInfoDialog();
                MessageToast.show(
                    sMode === "update"
                        ? "Information updated successfully"
                        : "Information added successfully"
                );

                await this._loadOwnProfile();

            } catch (oError) {
                MessageBox.error("Cannot save personal information: " + oError.message);
            }
        },

        onLogout: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            MessageToast.show("Logged out successfully");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        onCreateDocument: function () {
            this.getOwnerComponent().getRouter().navTo("RouteCreateDocument");
        },

        onOpenDocument: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oDocument = oContext.getObject();
            this.getOwnerComponent().getRouter().navTo("RouteDocumentDetails", {
                documentId: String(oDocument.id)
            });
        },

        onOpenPersonWork: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oUser = oContext.getObject();
            this._openUserDetailsDialog(oUser.id);
        },

        onOpenSearchUser: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oUser = oContext.getObject();
            this._openUserDetailsDialog(oUser.id);
        },

        onSearchDashboard: function (oEvent) {
            var sValue = oEvent.getSource().getValue();
            this.getView().getModel("dashboard").setProperty("/search", sValue);
            this._runGlobalSearch(sValue);
        },

        onSearchLiveChange: function (oEvent) {
            var sValue = oEvent.getParameter("value");
            this.getView().getModel("dashboard").setProperty("/search", sValue);
            this._runGlobalSearch(sValue);
        }
    });
});