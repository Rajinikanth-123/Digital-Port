import { api } from "./api/api.js";

const tabLogin = document.querySelector("#tab-login");
const tabRegister = document.querySelector("#tab-register");
const loginForm = document.querySelector("#loginForm");
const registerForm = document.querySelector("#registerForm");
const errorBanner = document.querySelector("#auth-error");

tabLogin.addEventListener("click", () => {
  tabLogin.classList.add("active");
  tabRegister.classList.remove("active");
  loginForm.hidden = false;
  registerForm.hidden = true;
  errorBanner.hidden = true;
});

tabRegister.addEventListener("click", () => {
  tabRegister.classList.add("active");
  tabLogin.classList.remove("active");
  registerForm.hidden = false;
  loginForm.hidden = true;
  errorBanner.hidden = true;
});

loginForm.addEventListener("submit", async event => {
  event.preventDefault();
  errorBanner.hidden = true;

  const email = document.querySelector("#login-email").value.trim();
  const password = document.querySelector("#login-password").value;
  const submitBtn = document.querySelector("#btn-login");

  submitBtn.disabled = true;
  submitBtn.innerHTML = `<span class="loading-spinner"></span> Signing in...`;

  try {
    const user = await api.post("auth/login", { email, password });
    sessionStorage.setItem("harbor_user", JSON.stringify(user));
    location.href = "dashboard.html";
  } catch (error) {
    errorBanner.textContent = error.message || "Invalid email or password.";
    errorBanner.hidden = false;
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Sign in →";
  }
});

registerForm.addEventListener("submit", async event => {
  event.preventDefault();
  errorBanner.hidden = true;

  const name = document.querySelector("#reg-name").value.trim();
  const email = document.querySelector("#reg-email").value.trim();
  const password = document.querySelector("#reg-password").value;
  const submitBtn = document.querySelector("#btn-register");

  submitBtn.disabled = true;
  submitBtn.innerHTML = `<span class="loading-spinner"></span> Creating account...`;

  try {
    const user = await api.post("auth/register", { name, email, password });
    alert(`Account created successfully for ${user.name}! Please sign in.`);
    document.querySelector("#login-email").value = email;
    tabLogin.click();
  } catch (error) {
    errorBanner.textContent = error.message || "Registration failed. Please check your details.";
    errorBanner.hidden = false;
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Register Account →";
  }
});
