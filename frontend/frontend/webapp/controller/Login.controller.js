sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/core/Fragment",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, Fragment, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.Login", {

        onInit: function () {
            var sToken = localStorage.getItem("token");
            if (!sToken) {
                return;
            }

            var oStoredUser = null;

            try {
                oStoredUser = JSON.parse(localStorage.getItem("user"));
            } catch (e) {
                oStoredUser = null;
            }

            if (!oStoredUser || !oStoredUser.systemRole) {
                localStorage.removeItem("token");
                localStorage.removeItem("user");
                return;
            }

            if (oStoredUser.systemRole === "ADMIN") {
                this.getOwnerComponent().getRouter().navTo("RouteAdminDashboard");
            } else {
                this.getOwnerComponent().getRouter().navTo("RouteDashboard");
            }
        },

        onLogin: async function () {
            var sUsernameOrEmail = this.byId("loginUsernameInput").getValue().trim();
            var sPassword = this.byId("loginPasswordInput").getValue();

            if (!sUsernameOrEmail || !sPassword) {
                MessageToast.show("Please fill in username/email and password");
                return;
            }

            try {
                const oResponse = await fetch("http://localhost:8080/api/auth/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        usernameOrEmail: sUsernameOrEmail,
                        password: sPassword
                    })
                });

                const sResponseText = await oResponse.text();

                if (!oResponse.ok) {
                    MessageBox.error("Login failed: " + (sResponseText || "Unknown error"));
                    return;
                }

                var oData = sResponseText ? JSON.parse(sResponseText) : null;

                if (!oData || !oData.token || !oData.systemRole) {
                    MessageBox.error("Invalid login response from backend");
                    return;
                }

                var oUserData = {
                    id: oData.id || null,
                    username: oData.username || sUsernameOrEmail,
                    email: oData.email || "",
                    systemRole: oData.systemRole
                };

                localStorage.setItem("token", oData.token);
                localStorage.setItem("user", JSON.stringify(oUserData));

                this.byId("loginUsernameInput").setValue("");
                this.byId("loginPasswordInput").setValue("");

                MessageToast.show("Login successful");

                if (oUserData.systemRole === "ADMIN") {
                    this.getOwnerComponent().getRouter().navTo("RouteAdminDashboard");
                } else {
                    this.getOwnerComponent().getRouter().navTo("RouteDashboard");
                }
            } catch (oError) {
                MessageBox.error("Cannot connect to backend: " + oError.message);
            }
        },

        onGoToRegister: function () {
            this.getOwnerComponent().getRouter().navTo("RouteRegister");
        },

        onForgotPassword: async function () {
            try {
                if (!this._oForgotPasswordDialog) {
                    this._oForgotPasswordDialog = await Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.ForgotPasswordDialog",
                        controller: this
                    });

                    this.getView().addDependent(this._oForgotPasswordDialog);
                }

                this._oForgotPasswordDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot open dialog: " + oError.message);
            }
        },

        onCloseForgotPasswordDialog: function () {
            if (this._oForgotPasswordDialog) {
                this._oForgotPasswordDialog.close();
            }
        },

        _getForgotPasswordField: function (sFieldId) {
            return Fragment.byId(this.getView().getId(), sFieldId);
        },

        _clearForgotPasswordForm: function () {
            this._getForgotPasswordField("forgotUsernameOrEmailInput").setValue("");
            this._getForgotPasswordField("forgotNewPasswordInput").setValue("");
            this._getForgotPasswordField("forgotConfirmPasswordInput").setValue("");
        },

        onSubmitForgotPassword: async function () {
            try {
                var oUsernameOrEmailInput = this._getForgotPasswordField("forgotUsernameOrEmailInput");
                var oNewPasswordInput = this._getForgotPasswordField("forgotNewPasswordInput");
                var oConfirmPasswordInput = this._getForgotPasswordField("forgotConfirmPasswordInput");

                var sUsernameOrEmail = oUsernameOrEmailInput.getValue().trim();
                var sNewPassword = oNewPasswordInput.getValue();
                var sConfirmPassword = oConfirmPasswordInput.getValue();

                if (!sUsernameOrEmail || !sNewPassword || !sConfirmPassword) {
                    MessageBox.error("Please fill all fields");
                    return;
                }

                if (sNewPassword !== sConfirmPassword) {
                    MessageBox.error("Passwords do not match");
                    return;
                }

                const oResponse = await fetch("http://localhost:8080/api/auth/forgot-password", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        usernameOrEmail: sUsernameOrEmail,
                        newPassword: sNewPassword,
                        confirmPassword: sConfirmPassword
                    })
                });

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    MessageBox.error(sText || "Error while changing password");
                    return;
                }

                MessageToast.show(sText || "Password changed successfully");
                this._clearForgotPasswordForm();
                this.onCloseForgotPasswordDialog();

            } catch (oError) {
                MessageBox.error("Backend error: " + oError.message);
            }
        }
    });
});