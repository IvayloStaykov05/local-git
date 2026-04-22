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
    "sap/m/SegmentedButtonItem",
    "sap/m/TextArea"
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
    SegmentedButtonItem,
    TextArea
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
                currentUsername: "",
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

        _getAuthHeaders: function () {
            return {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            };
        },

        _getLoggedUser: function () {
            try {
                return JSON.parse(localStorage.getItem("user") || "{}");
            } catch (e) {
                return {};
            }
        },

        _loadDocument: async function (sDocumentId) {
            try {
                var oResponse = await fetch("http://localhost:8080/api/documents/" + sDocumentId, {
                    method: "GET",
                    headers: this._getAuthHeaders()
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Document loading failed");
                }

                var oDocument = sText ? JSON.parse(sText) : {};
                var oLoggedUser = this._getLoggedUser();

                this.getView().getModel("document").setData({
                    id: oDocument.id || null,
                    title: oDocument.title || "",
                    description: oDocument.description || "",
                    createdBy: oDocument.createdBy || "",
                    currentUserRole: oDocument.currentUserRole || "",
                    currentUsername: oLoggedUser.username || "",
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
            try {
                var oResponse = await fetch("http://localhost:8080/api/documentVersions/history", {
                    method: "POST",
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        documentId: iDocumentId
                    })
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Version history loading failed");
                }

                var aVersions = sText ? JSON.parse(sText) : [];
                this.getView().getModel("document").setProperty("/versions", Array.isArray(aVersions) ? aVersions : []);
            } catch (oError) {
                MessageBox.error("Неуспешно зареждане на версиите: " + oError.message);
            }
        },

        onAddVersionPress: function () {
            var sRole = this.getView().getModel("document").getProperty("/currentUserRole");

            if (sRole !== "OWNER" && sRole !== "AUTHOR") {
                MessageBox.warning("Нямаш право да добавяш нова версия.");
                return;
            }

            if (!this._oVersionFileInput) {
                this._oVersionFileInput = document.createElement("input");
                this._oVersionFileInput.type = "file";
                this._oVersionFileInput.style.display = "none";

                this._oVersionFileInput.addEventListener("change", function (oEvent) {
                    var oFile = oEvent.target.files && oEvent.target.files[0];
                    if (oFile) {
                        this._uploadNewVersion(oFile);
                    }
                    oEvent.target.value = "";
                }.bind(this));

                document.body.appendChild(this._oVersionFileInput);
            }

            this._oVersionFileInput.click();
        },

        _uploadNewVersion: async function (oFile) {
            var oDocumentModel = this.getView().getModel("document");
            var iDocumentId = oDocumentModel.getProperty("/id");

            if (!iDocumentId) {
                MessageBox.error("Липсва document id.");
                return;
            }

            if (!oFile) {
                MessageBox.warning("Избери файл.");
                return;
            }

            var oFormData = new FormData();
            oFormData.append("documentId", iDocumentId);
            oFormData.append("file", oFile);

            try {
                var oResponse = await fetch("http://localhost:8080/api/documentVersions/createNew", {
                    method: "POST",
                    headers: {
                        "Authorization": "Bearer " + localStorage.getItem("token")
                    },
                    body: oFormData
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot create new version");
                }

                MessageToast.show("Новата версия е качена успешно.");
                await this._loadDocument(iDocumentId);
            } catch (oError) {
                MessageBox.error("Неуспешно качване на нова версия: " + oError.message);
            }
        },

        onOpenVersion: async function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();
            var iVersionId = oVersion.versionId;

            try {
                var oResponse = await fetch(
                    "http://localhost:8080/api/documentVersions/" + oDocument.id + "/" + iVersionId + "/download",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + localStorage.getItem("token")
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
            var iVersionId = oVersion.versionId;

            try {
                var oResponse = await fetch(
                    "http://localhost:8080/api/documentVersions/" + oDocument.id + "/" + iVersionId + "/download",
                    {
                        method: "GET",
                        headers: {
                            "Authorization": "Bearer " + localStorage.getItem("token")
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
            var sUrl = "http://localhost:8080/api/users/search";

            if (sQuery && sQuery.trim()) {
                sUrl += "?search=" + encodeURIComponent(sQuery.trim());
            }

            try {
                var oResponse = await fetch(sUrl, {
                    method: "GET",
                    headers: this._getAuthHeaders()
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
                    headers: this._getAuthHeaders(),
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
        },

        onRemoveMemberPress: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oMember = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();
            var that = this;

            MessageBox.confirm(
                "Сигурен ли си, че искаш да премахнеш ролята на " + oMember.username + " от този проект?",
                {
                    title: "Remove role",
                    actions: [MessageBox.Action.DELETE, MessageBox.Action.CANCEL],
                    emphasizedAction: MessageBox.Action.DELETE,
                    onClose: async function (sAction) {
                        if (sAction !== MessageBox.Action.DELETE) {
                            return;
                        }

                        await that._removeMember(oDocument.id, oMember.username);
                    }
                }
            );
        },

        _removeMember: async function (iDocumentId, sUsername) {
            try {
                var oResponse = await fetch("http://localhost:8080/api/documentMembers/deleteMember", {
                    method: "DELETE",
                    headers: this._getAuthHeaders(),
                    body: JSON.stringify({
                        documentId: iDocumentId,
                        username: sUsername
                    })
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot remove member");
                }

                MessageToast.show("Ролята е премахната успешно.");
                await this._loadDocument(iDocumentId);
            } catch (oError) {
                MessageBox.error("Неуспешно премахване на ролята: " + oError.message);
            }
        },

        onApproveVersionPress: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();

            if (oDocument.currentUserRole !== "REVIEWER") {
                MessageBox.warning("Само reviewer може да approve-ва версии.");
                return;
            }

            if (oVersion.status !== "DRAFT") {
                MessageBox.warning("Само draft версии могат да бъдат approve-нати.");
                return;
            }

            this._openApproveVersionDialog(oDocument.id, oVersion);
        },

        _openApproveVersionDialog: function (iDocumentId, oVersion) {
            var oCommentArea = new TextArea({
                width: "100%",
                rows: 5,
                growing: true,
                growingMaxLines: 8,
                placeholder: "Optional comment..."
            }).addStyleClass("versionDecisionTextArea");

            var oDialog = new Dialog({
                title: "Approve version " + oVersion.versionNumber,
                contentWidth: "520px",
                stretchOnPhone: true,
                content: [
                    new VBox({
                        items: [
                            new Text({
                                text: "Може да добавиш коментар, но не е задължително."
                            }).addStyleClass("versionDecisionDialogText"),
                            oCommentArea
                        ]
                    }).addStyleClass("versionDecisionDialogContent")
                ],
                beginButton: new Button({
                    text: "Approve",
                    press: async function () {
                        await this._submitApproveVersion(iDocumentId, oVersion.versionId, oCommentArea.getValue());
                        oDialog.close();
                    }.bind(this)
                }).addStyleClass("versionApproveDialogButton"),
                endButton: new Button({
                    text: "Cancel",
                    press: function () {
                        oDialog.close();
                    }
                }).addStyleClass("versionCancelDialogButton"),
                afterClose: function () {
                    oDialog.destroy();
                }
            });

            this.getView().addDependent(oDialog);
            oDialog.open();
        },

        _submitApproveVersion: async function (iDocumentId, iVersionId, sComment) {
            var sToken = localStorage.getItem("token");

            try {
                var oResponse = await fetch("http://localhost:8080/api/documentVersions/approve", {
                    method: "PATCH",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    },
                    body: JSON.stringify({
                        documentId: iDocumentId,
                        versionId: iVersionId,
                        comment: sComment ? sComment.trim() : null
                    })
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot approve version");
                }

                MessageToast.show("Версията е approve-ната успешно.");
                await this._loadDocument(iDocumentId);
            } catch (oError) {
                MessageBox.error("Неуспешен approve: " + oError.message);
            }
        },

        onRejectVersionPress: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("document");
            if (!oContext) {
                return;
            }

            var oVersion = oContext.getObject();
            var oDocument = this.getView().getModel("document").getData();

            if (oDocument.currentUserRole !== "REVIEWER") {
                MessageBox.warning("Само reviewer може да reject-ва версии.");
                return;
            }

            if (oVersion.status !== "DRAFT") {
                MessageBox.warning("Само draft версии могат да бъдат reject-нати.");
                return;
            }

            this._openRejectVersionDialog(oDocument.id, oVersion);
        },

        _openRejectVersionDialog: function (iDocumentId, oVersion) {
            var oReasonArea = new TextArea({
                width: "100%",
                rows: 5,
                growing: true,
                growingMaxLines: 8,
                placeholder: "Write rejection reason..."
            }).addStyleClass("versionDecisionTextArea");

            var oDialog = new Dialog({
                title: "Reject version " + oVersion.versionNumber,
                contentWidth: "520px",
                stretchOnPhone: true,
                content: [
                    new VBox({
                        items: [
                            new Text({
                                text: "Причината за reject е задължителна."
                            }).addStyleClass("versionDecisionDialogText"),
                            oReasonArea
                        ]
                    }).addStyleClass("versionDecisionDialogContent")
                ],
                beginButton: new Button({
                    text: "Reject",
                    press: async function () {
                        var sReason = oReasonArea.getValue();

                        if (!sReason || !sReason.trim()) {
                            MessageBox.warning("Трябва да въведеш причина за reject.");
                            return;
                        }

                        await this._submitRejectVersion(iDocumentId, oVersion.versionId, sReason.trim());
                        oDialog.close();
                    }.bind(this)
                }).addStyleClass("versionRejectDialogButton"),
                endButton: new Button({
                    text: "Cancel",
                    press: function () {
                        oDialog.close();
                    }
                }).addStyleClass("versionCancelDialogButton"),
                afterClose: function () {
                    oDialog.destroy();
                }
            });

            this.getView().addDependent(oDialog);
            oDialog.open();
        },

        _submitRejectVersion: async function (iDocumentId, iVersionId, sReason) {
            var sToken = localStorage.getItem("token");

            try {
                var oResponse = await fetch("http://localhost:8080/api/documentVersions/reject", {
                    method: "PATCH",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    },
                    body: JSON.stringify({
                        documentId: iDocumentId,
                        versionId: iVersionId,
                        reason: sReason
                    })
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Cannot reject version");
                }

                MessageToast.show("Версията е reject-ната успешно.");
                await this._loadDocument(iDocumentId);
            } catch (oError) {
                MessageBox.error("Неуспешен reject: " + oError.message);
            }
        },

        formatCanRemoveMember: function (sCurrentRole, sMemberUsername, sOwnerUsername, sCurrentUsername) {
            var bCanManage = sCurrentRole === "OWNER" || sCurrentRole === "ADMIN";

            if (!bCanManage) {
                return false;
            }

            if (!sMemberUsername) {
                return false;
            }

            if (sMemberUsername === sOwnerUsername) {
                return false;
            }

            if (sMemberUsername === sCurrentUsername) {
                return false;
            }

            return true;
        },

        formatReviewerActionVisible: function (sCurrentUserRole, sVersionStatus) {
            return sCurrentUserRole === "REVIEWER" && sVersionStatus === "DRAFT";
        },

        formatApprovedByTextVisible: function (sApprovedBy) {
            return !!sApprovedBy;
        },

        formatRejectedByTextVisible: function (sRejectedBy) {
            return !!sRejectedBy;
        }
    });
});