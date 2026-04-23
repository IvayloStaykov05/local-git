sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/core/Fragment",
    "sap/m/Dialog",
    "sap/m/Button",
    "sap/m/Input",
    "sap/m/VBox",
    "sap/m/Text"
], function (Controller, JSONModel, MessageToast, MessageBox, Fragment, Dialog, Button, Input, VBox, Text) {
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
            this.getOwnerComponent().getRouter().getRoute("RouteDashboard")
                .attachPatternMatched(this._onRouteMatched, this);
            this._loadOwnProfile();
            this._loadDashboardData();
        },

        _onRouteMatched: function () {
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

        _mapNotification: function (oNotification) {
            var sType = oNotification.type || "";
            var sSender = oNotification.sender || "";
            var sDocumentTitle = oNotification.documentTitle || "";
            var sRole = oNotification.role || "";

            return {
                id: oNotification.id,
                message: oNotification.message || "",
                type: sType,
                isRead: !!oNotification.read,
                sender: sSender,
                createdAt: oNotification.createdAt || null,
                actionable: !!oNotification.actionable,
                invitationId: oNotification.invitationId || null,
                documentId: oNotification.documentId || null,
                documentTitle: sDocumentTitle,
                role: sRole,
                title: this._buildNotificationTitle(sType, sSender, sDocumentTitle, sRole),
                icon: this._buildNotificationIcon(sType),
                acceptText: sType === "ADMIN_REQUEST" ? "Create admin" : "Accept",
                rejectText: "Reject"
            };
        },

        _buildNotificationTitle: function (sType, sSender, sDocumentTitle, sRole) {
            if (sType === "ROLE_REQUEST") {
                return "Project invitation" + (sDocumentTitle ? ": " + sDocumentTitle : "");
            }

            if (sType === "ADMIN_REQUEST") {
                return "Admin invitation from " + sSender;
            }

            if (sType === "ROLE_ACCEPTED") {
                return sSender + " accepted your invitation";
            }

            if (sType === "ROLE_REJECTED") {
                return sSender + " rejected your invitation";
            }

            if (sType === "ADMIN_ACCEPTED") {
                return sSender + " accepted admin invitation";
            }

            if (sType === "ADMIN_REJECTED") {
                return sSender + " rejected admin invitation";
            }

            if (sType === "VERSION_CREATED") {
                return "New version created";
            }

            if (sType === "VERSION_APPROVED") {
                return "Version approved";
            }

            if (sType === "VERSION_REJECTED") {
                return "Version rejected";
            }

            return (sSender ? sSender + ": " : "") + (sRole || "Notification");
        },

        _buildNotificationIcon: function (sType) {
            switch (sType) {
            case "ROLE_REQUEST":
                return "👥";
            case "ADMIN_REQUEST":
                return "⭐";
            case "ROLE_ACCEPTED":
            case "ADMIN_ACCEPTED":
            case "VERSION_APPROVED":
                return "✅";
            case "ROLE_REJECTED":
            case "ADMIN_REJECTED":
            case "VERSION_REJECTED":
                return "❌";
            case "VERSION_CREATED":
                return "📄";
            default:
                return "🔔";
            }
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
                const [oDocumentsResponse, oSharedUsersResponse, oNotificationsResponse] = await Promise.all([
                    fetch("http://localhost:8080/api/documents/my", {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/documentMembers/shared-users", {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/notifications", {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    })
                ]);

                if (
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403 ||
                    oSharedUsersResponse.status === 401 || oSharedUsersResponse.status === 403 ||
                    oNotificationsResponse.status === 401 || oNotificationsResponse.status === 403
                ) {
                    this._handleUnauthorized();
                    return;
                }

                const sDocumentsText = await oDocumentsResponse.text();
                const sSharedUsersText = await oSharedUsersResponse.text();
                const sNotificationsText = await oNotificationsResponse.text();

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Documents loading failed");
                }

                if (!oSharedUsersResponse.ok) {
                    throw new Error(sSharedUsersText || "Shared users loading failed");
                }

                if (!oNotificationsResponse.ok) {
                    throw new Error(sNotificationsText || "Notifications loading failed");
                }

                var aDocuments = sDocumentsText ? JSON.parse(sDocumentsText) : [];
                var aSharedUsers = sSharedUsersText ? JSON.parse(sSharedUsersText) : [];
                var aNotifications = sNotificationsText ? JSON.parse(sNotificationsText) : [];

                var aMappedUsers = (Array.isArray(aSharedUsers) ? aSharedUsers : []).map(this._mapUser);
                var aMappedNotifications = (Array.isArray(aNotifications) ? aNotifications : []).map(this._mapNotification.bind(this));

                this.getView().getModel("dashboard").setProperty("/myDocuments", Array.isArray(aDocuments) ? aDocuments : []);
                this.getView().getModel("dashboard").setProperty("/peopleWithWork", aMappedUsers);
                this.getView().getModel("dashboard").setProperty("/notifications", aMappedNotifications);

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

        onNotificationAccept: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oNotification = oContext.getObject();

            if (!oNotification.actionable || !oNotification.invitationId) {
                MessageBox.warning("This notification is no longer actionable.");
                return;
            }

            if (oNotification.type === "ROLE_REQUEST") {
                this._acceptDocumentInvitation(oNotification);
                return;
            }

            if (oNotification.type === "ADMIN_REQUEST") {
                this._openAdminAcceptDialog(oNotification);
            }
        },

        onNotificationReject: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oNotification = oContext.getObject();

            if (!oNotification.actionable || !oNotification.invitationId) {
                MessageBox.warning("This notification is no longer actionable.");
                return;
            }

            if (oNotification.type === "ROLE_REQUEST") {
                this._rejectDocumentInvitation(oNotification);
                return;
            }

            if (oNotification.type === "ADMIN_REQUEST") {
                this._rejectAdminInvitation(oNotification);
            }
        },

        _acceptDocumentInvitation: async function (oNotification) {
            try {
                var oResponse = await fetch("http://localhost:8080/api/invitations/" + oNotification.invitationId + "/accept", {
                    method: "POST",
                    headers: this._getAuthHeaders()
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot accept invitation");
                }

                await this._markNotificationAsRead(oNotification.id);
                MessageToast.show("Invitation accepted successfully.");
                await this._loadDashboardData();
            } catch (oError) {
                MessageBox.error("Cannot accept invitation: " + oError.message);
            }
        },

        _rejectDocumentInvitation: async function (oNotification) {
            try {
                var oResponse = await fetch("http://localhost:8080/api/invitations/" + oNotification.invitationId + "/reject", {
                    method: "POST",
                    headers: this._getAuthHeaders()
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot reject invitation");
                }

                await this._markNotificationAsRead(oNotification.id);
                MessageToast.show("Invitation rejected successfully.");
                await this._loadDashboardData();
            } catch (oError) {
                MessageBox.error("Cannot reject invitation: " + oError.message);
            }
        },

        _openAdminAcceptDialog: function (oNotification) {
            var oUsernameInput = new Input({
                width: "100%",
                placeholder: "Admin username"
            });

            var oEmailInput = new Input({
                width: "100%",
                type: "Email",
                placeholder: "Admin email"
            });

            var oPasswordInput = new Input({
                width: "100%",
                type: "Password",
                placeholder: "Admin password"
            });

            var oDialog = new Dialog({
                title: "Create admin profile",
                contentWidth: "480px",
                stretchOnPhone: true,
                content: [
                    new VBox({
                        width: "100%",
                        items: [
                            new Text({
                                text: "To accept the admin invitation, complete the data for the new admin profile."
                            }),
                            oUsernameInput,
                            oEmailInput,
                            oPasswordInput
                        ]
                    })
                ],
                beginButton: new Button({
                    text: "Create admin",
                    press: async function () {
                        var sAdminUsername = (oUsernameInput.getValue() || "").trim();
                        var sAdminEmail = (oEmailInput.getValue() || "").trim();
                        var sAdminPassword = (oPasswordInput.getValue() || "").trim();

                        if (!sAdminUsername || !sAdminEmail || !sAdminPassword) {
                            MessageBox.warning("All fields are required.");
                            return;
                        }

                        try {
                            await this._acceptAdminInvitation(oNotification, {
                                adminUsername: sAdminUsername,
                                adminEmail: sAdminEmail,
                                adminPassword: sAdminPassword
                            });
                            oDialog.close();
                        } catch (oError) {
                            MessageBox.error("Cannot create admin profile: " + oError.message);
                        }
                    }.bind(this)
                }),
                endButton: new Button({
                    text: "Cancel",
                    press: function () {
                        oDialog.close();
                    }
                }),
                afterClose: function () {
                    oDialog.destroy();
                }
            });

            this.getView().addDependent(oDialog);
            oDialog.open();
        },

        _acceptAdminInvitation: async function (oNotification, oPayload) {
            var oResponse = await fetch("http://localhost:8080/api/admin-invitations/" + oNotification.invitationId + "/accept", {
                method: "POST",
                headers: this._getAuthHeaders(),
                body: JSON.stringify(oPayload)
            });

            var sText = await oResponse.text();
            if (!oResponse.ok) {
                throw new Error(sText || "Cannot accept admin invitation");
            }

            await this._markNotificationAsRead(oNotification.id);
            MessageToast.show("Admin profile created successfully.");
            await this._loadDashboardData();
        },

        _rejectAdminInvitation: async function (oNotification) {
            try {
                var oResponse = await fetch("http://localhost:8080/api/admin-invitations/" + oNotification.invitationId + "/reject", {
                    method: "POST",
                    headers: this._getAuthHeaders()
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot reject admin invitation");
                }

                await this._markNotificationAsRead(oNotification.id);
                MessageToast.show("Admin invitation rejected successfully.");
                await this._loadDashboardData();
            } catch (oError) {
                MessageBox.error("Cannot reject admin invitation: " + oError.message);
            }
        },

        _markNotificationAsRead: async function (iNotificationId) {
            if (!iNotificationId) {
                return;
            }

            var oResponse = await fetch("http://localhost:8080/api/notifications/" + iNotificationId + "/read", {
                method: "PATCH",
                headers: this._getAuthHeaders()
            });

            if (oResponse.status === 401 || oResponse.status === 403) {
                this._handleUnauthorized();
                return;
            }

            if (!oResponse.ok) {
                var sText = await oResponse.text();
                throw new Error(sText || "Cannot mark notification as read");
            }
        },

        onMarkAllNotificationsRead: async function () {
            try {
                var oResponse = await fetch("http://localhost:8080/api/notifications/read-all", {
                    method: "PATCH",
                    headers: this._getAuthHeaders()
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot mark notifications as read");
                }

                MessageToast.show("All notifications marked as read.");
                await this._loadDashboardData();
            } catch (oError) {
                MessageBox.error("Cannot mark notifications as read: " + oError.message);
            }
        },

        formatNotificationActionsVisible: function (bActionable) {
            return !!bActionable;
        },

        formatNotificationReadText: function (bIsRead) {
            return bIsRead ? "Read" : "Unread";
        },

        formatNotificationReadState: function (bIsRead) {
            return bIsRead ? "Success" : "Information";
        },

        formatNotificationDate: function (sCreatedAt) {
            if (!sCreatedAt) {
                return "";
            }

            var oDate = new Date(sCreatedAt);
            if (isNaN(oDate.getTime())) {
                return sCreatedAt;
            }

            return oDate.toLocaleString();
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