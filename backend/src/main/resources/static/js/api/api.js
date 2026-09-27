import { APP_CONFIG } from "../config.js";

function getCookie(name) {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp("(^|; )" + name + "=([^;]+)"));
  return match ? decodeURIComponent(match[2]) : null;
}

class ApiClient {
  constructor() {
    this.baseUrl = APP_CONFIG.apiBaseUrl;
  }

  setBaseUrl(url) {
    this.baseUrl = url;
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith("http")
      ? endpoint
      : `${this.baseUrl.replace(/\/$/, "")}/${endpoint.replace(/^\//, "")}`;

    const defaultHeaders = {
      "Accept": "application/json"
    };

    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken && options.method && options.method !== "GET" && options.method !== "HEAD") {
      defaultHeaders["X-XSRF-TOKEN"] = csrfToken;
    }

    if (options.body && !(options.body instanceof FormData)) {
      defaultHeaders["Content-Type"] = "application/json";
    }

    const config = {
      ...options,
      credentials: options.credentials || "include",
      headers: {
        ...defaultHeaders,
        ...options.headers
      }
    };

    if (config.body && typeof config.body === "object" && !(config.body instanceof FormData)) {
      config.body = JSON.stringify(config.body);
    }

    try {
      const response = await fetch(url, config);

      if (response.status === 204) {
        return null;
      }

      const contentType = response.headers.get("content-type");
      let data = null;
      if (contentType && contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = text ? { message: text } : null;
      }

      if (!response.ok) {
        let errorMsg = (data && data.message) || `HTTP ${response.status} ${response.statusText}`;
        if (data && data.details && typeof data.details === "object") {
          const detailMsgs = Object.entries(data.details).map(([k, v]) => `${k}: ${v}`).join(", ");
          if (detailMsgs) errorMsg += ` (${detailMsgs})`;
        }
        const err = new Error(errorMsg);
        err.status = response.status;
        err.data = data;
        throw err;
      }

      return data;
    } catch (error) {
      if (error.status) throw error;
      throw new Error(`Network failure: Unable to connect to backend server at ${this.baseUrl}. Please ensure Spring Boot is running.`);
    }
  }

  async get(endpoint, params = {}) {
    let url = endpoint;
    const searchParams = new URLSearchParams();
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null && params[key] !== "") {
        searchParams.append(key, params[key]);
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes("?") ? "&" : "?") + queryString;
    }
    return this.request(url, { method: "GET" });
  }

  async post(endpoint, body) {
    return this.request(endpoint, { method: "POST", body });
  }

  async put(endpoint, body) {
    return this.request(endpoint, { method: "PUT", body });
  }

  async delete(endpoint) {
    return this.request(endpoint, { method: "DELETE" });
  }

  // Helper to extract list from Spring Data Page or Array
  unwrap(data) {
    if (!data) return [];
    if (Array.isArray(data)) return data;
    if (Array.isArray(data.content)) return data.content;
    return [data];
  }
}

export const api = new ApiClient();
