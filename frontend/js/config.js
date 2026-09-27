export const APP_CONFIG = {
  name: "HarborOS",
  apiBaseUrl: typeof window !== "undefined" && window.location.origin.includes("8080") ? "/api" : "http://localhost:8080/api",
  demoMode: false
};

export const DEMO_DATA = {
  stats: [
    ["Total Ports", "08", "↑ 12.5%", "teal"],
    ["Total Terminals", "24", "↑ 8.2%", "blue"],
    ["Total Vessels", "126", "↑ 5.7%", "purple"],
    ["Total Containers", "8,492", "↑ 14.1%", "orange"],
    ["Active Shipments", "1,284", "↑ 9.4%", "teal"],
    ["Available Berths", "18 / 32", "56% available", "blue"],
    ["Pending Customs", "47", "↓ 6.3%", "orange"],
    ["Total Revenue", "$2.84M", "↑ 18.6%", "purple"]
  ],
  vessels: [
    ["MV Ocean Meridian", "IMO 9784321", "Colombo", "27 Sep, 08:30", "Underway"],
    ["Pacific Dawn", "IMO 9658210", "Singapore", "27 Sep, 14:00", "At berth"],
    ["Aurora Clipper", "IMO 9912044", "Rotterdam", "28 Sep, 11:15", "Underway"],
    ["Blue Horizon", "IMO 9421108", "Valencia", "29 Sep, 06:45", "Delayed"]
  ],
  shipments: [
    ["DPS-2026-0001", "Northstar Logistics", "Shanghai → Rotterdam", "In transit"],
    ["DPS-2026-0002", "Atlas Imports", "Singapore → Antwerp", "Customs hold"],
    ["DPS-2026-0003", "Bluewater Co.", "Busan → Valencia", "In transit"],
    ["DPS-2026-0004", "Meridian Retail", "Dubai → Rotterdam", "Delivered"]
  ]
};
