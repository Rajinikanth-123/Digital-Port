export const ALL_NAV_ITEMS = [
  ["dashboard.html", "▦", "Dashboard", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["ports.html", "⌖", "Port Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["terminals.html", "▤", "Terminal Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["vessels.html", "◉", "Vessel Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["berths.html", "◫", "Berth Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["containers.html", "▣", "Container Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["cargo.html", "◈", "Cargo Management", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["shipments.html", "⇄", "Shipment Tracking", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["schedules.html", "◷", "Shipping Schedules", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["customs.html", "✓", "Customs Clearance", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["documents.html", "▤", "Documents", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["invoices.html", "$", "Invoices", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["payments.html", "◒", "Payments", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["notifications.html", "🔔", "Notifications", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["users.html", "♙", "User Management", ["ADMIN"]],
  ["audit-logs.html", "📋", "Audit Logs", ["ADMIN"]],
  ["search.html", "⌕", "Global Search", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["reports.html", "▥", "Reports", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["profile.html", "◎", "Profile", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["help.html", "❓", "Help & Support", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]],
  ["login.html", "⇥", "Logout", ["ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER"]]
];

export function currentPage() {
  return location.pathname.split("/").pop() || "index.html";
}

export function getFilteredNavItems(role = "VIEWER") {
  const userRole = (role || "VIEWER").toUpperCase().replace("ROLE_", "");
  return ALL_NAV_ITEMS.filter(([href, icon, label, roles]) => roles.includes(userRole));
}
