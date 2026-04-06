sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageBox",
    "sap/m/MessageToast",
    "sap/m/Dialog",
    "sap/m/Button",
    "sap/m/List",
    "sap/m/StandardListItem",
    "sap/m/SearchField",
    "sap/m/VBox",
    "sap/m/Text",
    "sap/m/HBox",
    "sap/m/SegmentedButton",
    "sap/m/SegmentedButtonItem"
], function (
    Controller,
    JSONModel,
    MessageBox,
    MessageToast,
    Dialog,
    Button,
    List,
    StandardListItem,
    SearchField,
    VBox,
    Text,
    HBox,
    SegmentedButton,
    SegmentedButtonItem
) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.DocumentDetails", {
        onInit: function () {
            this.getView().setModel(new JSONModel({
                id: null,
                title: "",
                description: "",
                createdBy: "",
                currentUserRole: "",
                activeVersionNumber: null,
                activeVersionId: null,
                activeFileName: "",
                activeContentType: "",
                activeFileSize: null,
                teamMembers: [],
                versions: [],
                activePreviewHtml: "<div class='documentPreviewPlaceholder'>Избери версия.</div>"
            }), "document");

            this.getView().setModel(new JSONModel({
                search: "",
                users: [],
                selectedUsername: "",
                selectedRole: "READER"
            }), "memberDialog");

            this.getOwnerComponent().getRouter().getRoute("RouteDocumentDetails")
                .attachPatternMatched(this._onRouteMatched, this);
        },

        onBackToDashboard: function () {
            this.getOwnerComponent().getRouter().navTo("RouteDashboard");
        },

        _onRouteMatched: function (oEvent) {
            var sDocumentId = oEvent.getParameter("arguments").documentId;
            this._loadDocument(sDocumentId);
        },

        _loadDocument: async function (sDocumentId) {
            var sToken = localStorage.getItem("token");

            try {
                var oResponse = await fetch("http://localhost:8080/api/documents/" + sDocumentId, {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    }
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Document loading failed");
                }

                var oDocument = sText ? JSON.parse(sText) : {};

                this.getView().getModel("document").setData({
                    id: oDocument.id || null,
                    title: oDocument.title || "",
                    description: oDocument.description || "",
                    createdBy: oDocument.createdBy || "",
                    currentUserRole: oDocument.currentUserRole || "",
                    activeVersionNumber: oDocument.activeVersionNumber || null,
                    activeVersionId: oDocument.activeVersionId || null,
                    activeFileName: oDocument.activeFileName || "",
                    activeContentType: oDocument.activeContentType || "",
                    activeFileSize: oDocument.activeFileSize || null,
                    teamMembers: oDocument.teamMembers || [],
                    versions: [],
                    activePreviewHtml: "<div class='documentPreviewPlaceholder'>Избери версия.</div>"
                });

                await this._loadVersions(oDocument.id);
            } catch (oError) {
                MessageBox.error("Неуспешно зареждане: " + oError.message);
            }
        },

        _loadVersions: async function (iDocumentId) {
            var sToken = localStorage.getItem("token");

            try {
                var oResponse = await fetch("http://localhost:8080/api/documentVersions/history", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    },
                    body: JSON.stringify({ documentId: iDocumentId })
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Version history loading failed");
                }

                var aVersions = sText ? JSON.parse(sText) : [];
                this.getView().getModel("document").setProperty("/versions", aVersions);
            } catch (oError) {
                MessageBox.error("Неуспешно зареждане на версиите: " + oError.message);
            }
        },

        onOpenVersion: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();
            var sToken = localStorage.getItem("token");
            var iVersionId = oVersion.versionId;

            try {
                var oResponse = await fetch(
                    "http://localhost:8080/api/documentVersions/" + oDocument.id + "/" + iVersionId + "/download",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + sToken
                        }
                    }
                );

                if (!oResponse.ok) {
                    var sErrorText = await oResponse.text();
                    throw new Error(sErrorText || "Failed to open file");
                }

                var sContentType = oResponse.headers.get("Content-Type") || "";
                var sContentDisposition = oResponse.headers.get("Content-Disposition") || "";
                var sFileName = this._extractFileNameFromDisposition(sContentDisposition) || ("version-" + oVersion.versionNumber);

                if (sContentType.includes("text/plain") || /\.txt$/i.test(sFileName)) {
                    var sText = await oResponse.text();

                    this.getView().getModel("document").setProperty(
                        "/activePreviewHtml",
                        this._buildTextPreviewHtml(sText)
                    );
                    this.getView().getModel("document").setProperty("/activeFileName", sFileName);
                    this.getView().getModel("document").setProperty("/activeContentType", sContentType);

                    return;
                }

                var oBlob = await oResponse.blob();
                var sBlobUrl = URL.createObjectURL(oBlob);

                if (sContentType.includes("application/pdf") || /\.pdf$/i.test(sFileName)) {
                    window.open(sBlobUrl, "_blank");

                    setTimeout(function () {
                        URL.revokeObjectURL(sBlobUrl);
                    }, 10000);

                    return;
                }

                MessageToast.show("Този тип файл не може да се визуализира тук. Използвай Download.");
            } catch (oError) {
                MessageBox.error("Неуспешно отваряне: " + oError.message);
            }
        },

        onDownloadVersion: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();
            var sToken = localStorage.getItem("token");
            var iVersionId = oVersion.versionId;

            try {
                var oResponse = await fetch(
                    "http://localhost:8080/api/documentVersions/" + oDocument.id + "/" + iVersionId + "/download",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + sToken
                        }
                    }
                );

                if (!oResponse.ok) {
                    var sErrorText = await oResponse.text();
                    throw new Error(sErrorText || "Failed to download file");
                }

                var sContentDisposition = oResponse.headers.get("Content-Disposition") || "";
                var sFileName = this._extractFileNameFromDisposition(sContentDisposition) || ("version-" + oVersion.versionNumber);

                var oBlob = await oResponse.blob();
                var sBlobUrl = URL.createObjectURL(oBlob);

                var oLink = document.createElement("a");
                oLink.href = sBlobUrl;
                oLink.download = sFileName;
                document.body.appendChild(oLink);
                oLink.click();
                document.body.removeChild(oLink);

                URL.revokeObjectURL(sBlobUrl);
            } catch (oError) {
                MessageBox.error("Неуспешно теглене: " + oError.message);
            }
        },

        _extractFileNameFromDisposition: function (sDisposition) {
            if (!sDisposition) {
                return "";
            }

            var aUtfMatch = /filename\*=UTF-8''([^;]+)/i.exec(sDisposition);
            if (aUtfMatch && aUtfMatch[1]) {
                return decodeURIComponent(aUtfMatch[1]);
            }

            var aSimpleMatch = /filename=\"?([^\";]+)\"?/i.exec(sDisposition);
            if (aSimpleMatch && aSimpleMatch[1]) {
                return aSimpleMatch[1];
            }

            return "";
        },

        _escapeHtml: function (sValue) {
            if (sValue === null || sValue === undefined) {
                return "";
            }

            return String(sValue)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        },

        _buildTextPreviewHtml: function (sText) {
            return "<div class='documentPreviewText'><pre>" + this._escapeHtml(sText) + "</pre></div>";
        },

        onOpenAddMemberDialog: async function () {
            var sCurrentUserRole = this.getView().getModel("document").getProperty("/currentUserRole");

            if (sCurrentUserRole !== "OWNER") {
                MessageBox.warning("Само owner може да добавя хора.");
                return;
            }

            if (!this._oAddMemberDialog) {
                this._createAddMemberDialog();
            }

            this.getView().getModel("memberDialog").setData({
                search: "",
                users: [],
                selectedUsername: "",
                selectedRole: "READER"
            });

            await this._searchUsers("");
            this._oAddMemberDialog.open();
        },

        _createAddMemberDialog: function () {
            var oList = new List({
                mode: "SingleSelectMaster",
                growing: true,
                items: {
                    path: "memberDialog>/users",
                    template: new StandardListItem({
                        title: "{memberDialog>username}",
                        description: "{memberDialog>email}",
                        info: "{memberDialog>firstName} {memberDialog>lastName}"
                    })
                },
                selectionChange: function (oEvent) {
                    var oItem = oEvent.getParameter("listItem");
                    var oContext = oItem && oItem.getBindingContext("memberDialog");

                    if (oContext) {
                        this.getView().getModel("memberDialog")
                            .setProperty("/selectedUsername", oContext.getObject().username);
                    }
                }.bind(this)
            });

            this._oAddMemberDialog = new Dialog({
                title: "Добавяне на човек в екипа",
                contentWidth: "560px",
                contentHeight: "620px",
                stretchOnPhone: true,
                content: [
                    new VBox({
                        width: "100%",
                        items: [
                            new SearchField({
                                width: "100%",
                                placeholder: "Търси user",
                                liveChange: function (oEvent) {
                                    this._searchUsers(oEvent.getParameter("newValue") || "");
                                }.bind(this),
                                search: function (oEvent) {
                                    this._searchUsers(oEvent.getParameter("query") || "");
                                }.bind(this)
                            }).addStyleClass("memberDialogSearch"),

                            new Text({
                                text: "Избери роля"
                            }).addStyleClass("memberDialogLabel"),

                            new HBox({
                                width: "100%",
                                justifyContent: "Center",
                                items: [
                                    new SegmentedButton({
                                        width: "100%",
                                        selectedKey: "{memberDialog>/selectedRole}",
                                        selectionChange: function (oEvent) {
                                            this.getView().getModel("memberDialog")
                                                .setProperty("/selectedRole", oEvent.getParameter("item").getKey());
                                        }.bind(this),
                                        items: [
                                            new SegmentedButtonItem({
                                                key: "AUTHOR",
                                                text: "AUTHOR"
                                            }),
                                            new SegmentedButtonItem({
                                                key: "REVIEWER",
                                                text: "REVIEWER"
                                            }),
                                            new SegmentedButtonItem({
                                                key: "READER",
                                                text: "READER"
                                            })
                                        ]
                                    }).addStyleClass("memberRoleSegmentedButton")
                                ]
                            }).addStyleClass("memberRoleButtonsWrapper"),

                            new Text({
                                text: "Избери user"
                            }).addStyleClass("memberDialogLabel"),

                            oList
                        ]
                    }).addStyleClass("memberDialogContent")
                ],
                beginButton: new Button({
                    text: "Add",
                    press: this.onAddMember.bind(this)
                }),
                endButton: new Button({
                    text: "Cancel",
                    press: function () {
                        this._oAddMemberDialog.close();
                    }.bind(this)
                })
            });

            this.getView().addDependent(this._oAddMemberDialog);
        },

        _searchUsers: async function (sQuery) {
            var sToken = localStorage.getItem("token");
            var sUrl = "http://localhost:8080/api/users/search";

            if (sQuery && sQuery.trim()) {
                sUrl += "?search=" + encodeURIComponent(sQuery.trim());
            }

            try {
                var oResponse = await fetch(sUrl, {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    }
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "User search failed");
                }

                var aUsers = sText ? JSON.parse(sText) : [];
                var sOwner = this.getView().getModel("document").getProperty("/createdBy");
                var aTeamMembers = this.getView().getModel("document").getProperty("/teamMembers") || [];

                var aExistingUsernames = aTeamMembers.map(function (oMember) {
                    return oMember.username;
                });

                var aFilteredUsers = (Array.isArray(aUsers) ? aUsers : []).filter(function (oUser) {
                    return oUser.username !== sOwner && aExistingUsernames.indexOf(oUser.username) === -1;
                });

                this.getView().getModel("memberDialog").setProperty("/users", aFilteredUsers);
            } catch (oError) {
                MessageBox.error("Неуспешно търсене на users: " + oError.message);
            }
        },

        onAddMember: async function () {
            var oDialogModel = this.getView().getModel("memberDialog");
            var sUsername = oDialogModel.getProperty("/selectedUsername");
            var sRole = oDialogModel.getProperty("/selectedRole");
            var oDocument = this.getView().getModel("document").getData();
            var sToken = localStorage.getItem("token");

            if (!sUsername) {
                MessageBox.warning("Избери user от списъка.");
                return;
            }

            if (oDocument.currentUserRole !== "OWNER") {
                MessageBox.warning("Само owner може да добавя хора.");
                return;
            }

            try {
                var oResponse = await fetch("http://localhost:8080/api/documentMembers/createNewMember", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    },
                    body: JSON.stringify({
                        documentId: oDocument.id,
                        owner: oDocument.createdBy,
                        username: sUsername,
                        role: sRole
                    })
                });

                var sText = await oResponse.text();
                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot add member");
                }

                MessageToast.show("Човекът е добавен успешно.");
                this._oAddMemberDialog.close();

                await this._loadDocument(oDocument.id);
            } catch (oError) {
                MessageBox.error("Неуспешно добавяне на човек: " + oError.message);
            }
        }
    });
});