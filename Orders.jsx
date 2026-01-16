import React, { useEffect, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import OrderDetailModal from "./OrderDetail";
import "../css/Orders.css";

const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080/api";

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      
      if (!token) {
        toast.error("Bạn chưa đăng nhập!");
        return;
      }

      // ✅ Gọi endpoint GET /api/orders
      const response = await fetch(`${API_BASE}/orders`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 403) {
        toast.error("Bạn không có quyền xem danh sách đơn hàng!");
        return;
      }

      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status}`);
      }

      const data = await response.json();
      
      if (Array.isArray(data)) {
        setOrders(data);
      } else {
        toast.error("Dữ liệu trả về không hợp lệ!");
      }
    } catch (err) {
      console.error("Error fetching orders:", err);
      toast.error("Không thể tải danh sách đơn hàng!");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (orderId) => {
    if (!window.confirm(`Bạn có chắc muốn xóa đơn hàng #${orderId}?`)) return;

    try {
      const token = localStorage.getItem("token");
      
      const response = await fetch(`${API_BASE}/orders/${orderId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        toast.success(`Đã xóa đơn hàng #${orderId}!`);
        fetchOrders(); // Tải lại danh sách
      } else {
        toast.error("Xóa thất bại!");
      }
    } catch (err) {
      console.error(err);
      toast.error("Lỗi khi xóa đơn hàng!");
    }
  };

  // Status color
  const getStatusStyle = (status) => {
    switch (status) {
      case "PAID":
        return { color: "#28a745", text: "Đã thanh toán" };
      case "PENDING":
        return { color: "#ffc107", text: "Đang mở" };
      case "COMPLETED":
        return { color: "#17a2b8", text: "Hoàn thành" };
      case "PREPARING":
        return { color: "#fd7e14", text: "Đang chuẩn bị" };
      case "SERVED":
        return { color: "#20c997", text: "Đã phục vụ" };
      case "CANCELLED":
        return { color: "#dc3545", text: "Đã hủy" };
      default:
        return { color: "#6c757d", text: status };
    }
  };

  return (
    <div className="order-manager-container">
      <ToastContainer />
      <h2>📦 Quản lý Đơn hàng</h2>

      {loading && <p>Đang tải...</p>}

      {!loading && orders.length === 0 && <p>Không có đơn hàng nào.</p>}

      {!loading && orders.length > 0 && (
        <table className="order-table">
          <thead>
            <tr>
              <th>Mã Đơn</th>
              <th>Khách hàng</th>
              <th>Bàn</th>
              <th>Nhân viên</th>
              <th>Ngày tạo</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => {
              const statusInfo = getStatusStyle(order.status);
              return (
                <tr
                  key={order.id}
                  onClick={() => setSelectedOrder(order)}
                  className="order-row-clickable"
                >
                  <td>#{order.id}</td>
                  <td>{order.customerName || "N/A"}</td>
                  <td>{order.table?.number || "N/A"}</td>
                  <td>{order.user?.fullName || order.user?.username || "N/A"}</td>
                  <td>{new Date(order.createdAt).toLocaleString("vi-VN")}</td>
                  <td>{order.finalAmount?.toLocaleString("vi-VN")}₫</td>
                  <td>
                    <span style={{ color: statusInfo.color, fontWeight: 600 }}>
                      {statusInfo.text}
                    </span>
                  </td>
                  <td>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(order.id);
                      }}
                      className="btn-delete"
                    >
                      🗑️ Xóa
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      {selectedOrder && (
        <OrderDetailModal
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
        />
      )}
    </div>
  );
};

export default Orders;
