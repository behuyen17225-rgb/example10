import React, { useEffect, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";

const COLORS = ["#A0522D", "#28a745", "#007bff", "#ffc107", "#dc3545", "#6c757d"];

const TIME_RANGES = {
  "7 days": 7,
  "30 days": 30,
  "90 days": 90,
};

const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080/api";

const Report = () => {
  const [salesData, setSalesData] = useState([]);
  const [dailyData, setDailyData] = useState([]);
  const [categoryRevenueData, setCategoryRevenueData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [timeRange, setTimeRange] = useState(7);

  // ✅ Get token from localStorage
  const getAuthHeaders = () => {
    const token = localStorage.getItem("token");
    return {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
    };
  };

  // ✅ TOP SELLING PRODUCTS
  const fetchTopSellingStats = async (limit = 5) => {
    try {
      const response = await fetch(`${API_BASE}/orders/stats/top-selling?limit=${limit}`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        if (response.status === 403) {
          toast.error("Bạn không có quyền xem thống kê!");
        }
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      return Array.isArray(data) ? data : [];
    } catch (err) {
      console.error("Error fetching top selling stats:", err);
      return [];
    }
  };

  // ✅ DAILY REVENUE
  const fetchDailyRevenueStats = async (days = 7) => {
    try {
      const response = await fetch(`${API_BASE}/orders/stats/daily-revenue`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      return Array.isArray(data) ? data : [];
    } catch (err) {
      console.error("Error fetching daily revenue stats:", err);
      return [];
    }
  };

  // ✅ REVENUE BY CATEGORY
  const fetchRevenueByCategoryStats = async () => {
    try {
      const response = await fetch(`${API_BASE}/orders/stats/revenue-by-category`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      return Array.isArray(data) ? data : [];
    } catch (err) {
      console.error("Error fetching revenue by category stats:", err);
      return [];
    }
  };

  const fetchAnalytics = async (days) => {
    setIsLoading(true);
    try {
      // 1️⃣ Top Selling Products
      const salesRes = await fetchTopSellingStats(5);
      console.log("Top Selling API:", salesRes);
      const mappedSales = (salesRes || [])
        .map((item) => ({
          name: item.productName || "Unknown",
          "Số lượng bán": item.quantitySold || 0,
        }))
        .reverse();
      setSalesData(mappedSales);

      // 2️⃣ Daily Revenue
      const dailyRes = await fetchDailyRevenueStats(days);
      console.log("Daily Revenue API:", dailyRes);
      const mappedDaily = (dailyRes || []).map((item) => ({
        day: item.date ? new Date(item.date).toLocaleDateString("vi-VN") : "N/A",
        "Doanh thu": item.revenue || 0,
        "Số đơn": item.orderCount || 0,
      }));
      setDailyData(mappedDaily);

      // 3️⃣ Revenue by Category (Pie Chart)
      const categoryRes = await fetchRevenueByCategoryStats();
      console.log("Revenue By Category API:", categoryRes);
      const mappedCategoryRevenue = (categoryRes || []).map((item) => ({
        name: item.category || "Unknown",
        value: parseFloat(item.revenue) || 0,
      }));
      setCategoryRevenueData(mappedCategoryRevenue);
    } catch (err) {
      console.error("Error fetching analytics:", err);
      toast.error("Không thể tải dữ liệu thống kê!");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics(timeRange);
  }, [timeRange]);

  if (isLoading)
    return (
      <div style={{ paddingTop: "100px", textAlign: "center" }}>
        Đang tải dữ liệu biểu đồ...
      </div>
    );

  return (
    <div className="analytics-container" style={{ padding: "30px", minHeight: "100vh", backgroundColor: "#f5f5f5" }}>
      <ToastContainer />
      <h2 style={{ textAlign: "center", marginBottom: "30px", color: "#333" }}>
        📊 Bảng Điều Khiển Phân Tích Doanh Thu
      </h2>

      {/* Time Range Buttons */}
      <div style={{ marginBottom: "20px", textAlign: "center" }}>
        {Object.entries(TIME_RANGES).map(([label, days]) => (
          <button
            key={days}
            onClick={() => setTimeRange(days)}
            style={{
              padding: "8px 15px",
              margin: "0 5px",
              border: `2px solid ${timeRange === days ? "#a0522d" : "#ccc"}`,
              backgroundColor: timeRange === days ? "#a0522d" : "white",
              color: timeRange === days ? "white" : "#4a372d",
              borderRadius: "5px",
              cursor: "pointer",
              fontWeight: "bold",
              transition: "all 0.3s",
            }}
          >
            {label}
          </button>
        ))}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "30px", maxWidth: "1200px", margin: "0 auto" }}>
        {/* Line Chart: Daily Revenue & Orders */}
        <div style={{ backgroundColor: "#fff", padding: "20px", borderRadius: "10px", boxShadow: "0 4px 10px rgba(0,0,0,0.05)" }}>
          <h4 style={{ textAlign: "center", color: "#007bff", marginBottom: "15px" }}>
            📈 Doanh Thu & Số Đơn ({timeRange} Ngày Gần Nhất)
          </h4>
          {dailyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={dailyData} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis
                  yAxisId="left"
                  label={{ value: "Doanh thu (₫)", angle: -90, position: "insideLeft" }}
                  tickFormatter={(value) => (value / 1000000).toFixed(0) + "M"}
                />
                <YAxis
                  yAxisId="right"
                  orientation="right"
                  label={{ value: "Số đơn", angle: 90, position: "insideRight" }}
                  allowDecimals={false}
                />
                <Tooltip formatter={(value) => value.toLocaleString()} />
                <Legend />
                <Line yAxisId="left" type="monotone" dataKey="Doanh thu" stroke="#a0522d" strokeWidth={2} />
                <Line yAxisId="right" type="monotone" dataKey="Số đơn" stroke="#007bff" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <p style={{ textAlign: "center", color: "#999" }}>Không có dữ liệu</p>
          )}
        </div>

        {/* Bar Chart: Top Selling Products */}
        <div style={{ backgroundColor: "#fff", padding: "20px", borderRadius: "10px", boxShadow: "0 4px 10px rgba(0,0,0,0.05)" }}>
          <h4 style={{ textAlign: "center", color: "#28a745", marginBottom: "15px" }}>
            🏆 Top 5 Sản Phẩm Bán Chạy (Số lượng)
          </h4>
          {salesData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={salesData} layout="vertical" margin={{ top: 10, right: 30, left: 120, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" />
                <YAxis dataKey="name" type="category" width={110} />
                <Tooltip formatter={(value) => value.toLocaleString()} />
                <Bar dataKey="Số lượng bán" fill="#28a745" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p style={{ textAlign: "center", color: "#999" }}>Không có dữ liệu</p>
          )}
        </div>

        {/* Pie Chart: Revenue by Category */}
        <div style={{ backgroundColor: "#fff", padding: "20px", borderRadius: "10px", boxShadow: "0 4px 10px rgba(0,0,0,0.05)", gridColumn: "span 2" }}>
          <h4 style={{ textAlign: "center", color: "#a0522d", marginBottom: "15px" }}>
            🍩 Tỷ Lệ Doanh Thu theo Loại Món
          </h4>
          {categoryRevenueData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={categoryRevenueData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  fill="#8884d8"
                  label={({ name, percent }) => `${name} (${(percent * 100).toFixed(1)}%)`}
                >
                  {categoryRevenueData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => value.toLocaleString() + "₫"} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <p style={{ textAlign: "center", color: "#999" }}>Không có dữ liệu</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Report;
