sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, JSONModel, MessageToast, MessageBox) {
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
                    username: oStoredUser.username || "",
                    email: oStoredUser.email || "",
                    systemRole: oStoredUser.systemRole || ""
                },
                myDocuments: [],
                peopleWithWork: [],
                notifications: [],
                search: ""
            });

            this.getView().setModel(oDashboardModel, "dashboard");
            this._loadDashboardData("");
        },

        _loadDashboardData: async function (sSearch) {
            var sToken = localStorage.getItem("token");

            try {
                var sDocumentsUrl = "http://localhost:8080/api/documents/my";
                var sUsersUrl = "http://localhost:8080/api/users/search";
                
                if (sSearch && sSearch.trim()) {
                    var sEncodedSearch = encodeURIComponent(sSearch.trim());
                    sDocumentsUrl += "?search=" + sEncodedSearch;
                    sUsersUrl += "?search=" + sEncodedSearch;
                }

                const [oDocumentsResponse, oUsersResponse] = await Promise.all([
                    fetch(sDocumentsUrl, {
                        method: "GET",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": "Bearer " + sToken
                        }
                    }),
                    fetch(sUsersUrl, {
                        method: "GET",
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": "Bearer " + sToken
                        }
                    })
                ]);

                if (
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403 ||
                    oUsersResponse.status === 401 || oUsersResponse.status === 403
                ) {
                    localStorage.removeItem("token");
                    localStorage.removeItem("user");
                    this.getOwnerComponent().getRouter().navTo("RouteLogin");
                    return;
                }

                const sDocumentsText = await oDocumentsResponse.text();
                const sUsersText = await oUsersResponse.text();

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Documents loading failed");
                }

                if (!oUsersResponse.ok) {
                    throw new Error(sUsersText || "Users loading failed");
                }

                var aDocuments = sDocumentsText ? JSON.parse(sDocumentsText) : [];
                var aUsers = sUsersText ? JSON.parse(sUsersText) : [];

                var aMappedUsers = (Array.isArray(aUsers) ? aUsers : []).map(function (oUser) {
                    var sFirstName = oUser.firstName || "";
                    var sLastName = oUser.lastName || "";
                    var sUsername = oUser.username || "";
                    var sEmail = oUser.email || "";
                    var sName = (sFirstName + " " + sLastName).trim() || sUsername;

                    return {
                        id: oUser.id,
                        username: sUsername,
                        firstName: sFirstName,
                        lastName: sLastName,
                        email: sEmail,
                        name: sName,
                        review: sEmail,
                        initials: ((sFirstName.charAt(0) || "") + (sLastName.charAt(0) || "") || sUsername.substring(0, 2)).toUpperCase()
                    };
                });

                this.getView().getModel("dashboard").setProperty("/myDocuments", Array.isArray(aDocuments) ? aDocuments : []);
                this.getView().getModel("dashboard").setProperty("/peopleWithWork", aMappedUsers);

            } catch (oError) {
                MessageBox.error("Cannot load dashboard data: " + oError.message);
            }
        },

        onLogout: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            MessageToast.show("Logged out successfully");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        onCreateDocument: function () {
            MessageToast.show("Create document ще го вържем след това.");
        },

        onOpenDocument: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oDocument = oContext.getObject();
            MessageToast.show("Избран документ: " + (oDocument.title || ""));
        },

        onOpenPersonWork: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("dashboard");
            if (!oContext) {
                return;
            }

            var oUser = oContext.getObject();
            MessageToast.show("Избран user: " + (oUser.name || oUser.username || ""));
        },

        onSearchDashboard: function (oEvent) {
            var sValue = oEvent.getSource().getValue();
            this.getView().getModel("dashboard").setProperty("/search", sValue);
            this._loadDashboardData(sValue);
        },

        onSearchLiveChange: function (oEvent) {
            var sValue = oEvent.getParameter("value");
            this.getView().getModel("dashboard").setProperty("/search", sValue);
            this._loadDashboardData(sValue);
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
    });
});