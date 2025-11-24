<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
%>
<%-- THÊM CSS MỚI ĐỂ ĐỔI MÀU NÚT VÀ CẢI THIỆN BỐ CỤC HEADER --%>
<style>
    /* 🔥 BỔ SUNG: CSS RESET ĐỂ DÍNH SÁT LÊN TRÊN */
    body {
        margin: 0; /* Loại bỏ margin mặc định của body */
        padding: 0; /* Loại bỏ padding mặc định của body */
    }

    /* Định nghĩa màu xanh biển */
    :root {
        --blue-main: #007bff; /* Màu xanh biển chuẩn */
        --blue-hover: #0056b3; /* Màu xanh đậm hơn khi hover */
        --text-color-header: #333; /* Màu chữ tối trên nền trắng */
    }

    /* ĐIỀU CHỈNH CSS CHO HEADER ĐỂ CÓ MÀU NỀN TRẮNG */
    .header {
        background-color: white; /* Đặt nền trắng */
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); /* Thêm bóng nhẹ */
        width: 100%;
        position: relative;
        z-index: 100;
        margin: 0; /* Loại bỏ margin trên header (nếu có) */
    }

    /* BỔ SUNG: CSS CHO BỐ CỤC HEADER (Giúp nút không bị tràn) */
    .header-nav {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px 50px;
    }

    /* Đảm bảo navbar và actions cũng sử dụng flex hoặc căn chỉnh đúng */
    .navbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
    }

    .actions {
        display: flex;
        align-items: center;
        gap: 15px;
    }

    /* 1. Styling cho nút Đăng ký (Sign up) và Đăng xuất (Logout) */
    .btn-signup, .btn-logout {
        background-color: var(--blue-main) !important;
        color: white !important;
        border: 1px solid var(--blue-main) !important;
        padding: 8px 15px;
        border-radius: 5px;
        cursor: pointer;
        text-decoration: none;
        transition: all 0.3s ease;
        white-space: nowrap;
    }

    .btn-signup:hover, .btn-logout:hover {
        background-color: var(--blue-hover) !important;
        border-color: var(--blue-hover) !important;
    }

    /* 2. Styling cho chữ Đăng nhập (Login) */
    .text-login {
        color: var(--blue-main) !important;
        cursor: pointer;
        padding: 8px 10px;
        transition: color 0.3s ease;
        background-color: transparent !important;
        border: 1px solid var(--blue-main);
        border-radius: 5px;
        white-space: nowrap;
    }

    /* Khi hover vào chữ Login: Chuyển sang Xanh Đậm */
    .text-login:hover {
        color: var(--blue-hover) !important;
        border-color: var(--blue-hover) !important;
    }

    /* Đảm bảo chữ Hello User màu tối trên nền trắng */
    .actions label {
        color: var(--text-color-header);
        white-space: nowrap;
    }

    /* Căn chỉnh lại khoảng cách cho Hello User */
    .actions label span, .actions label b {
        margin-right: 5px;
    }
</style>
<%
    Boolean loginStatus = (Boolean) request.getSession().getAttribute("login-status");
    Boolean signUpStatus = (Boolean) request.getSession().getAttribute("signup-status");
%>
<div class="header header-nav">
    <div class="navbar">
        <div class="menu">
            <img class="logo-header" src="./img/Logo.png"
                 alt="PDF Convertion"
            />
        </div>
        <div class="actions">
            <%
                if ((loginStatus != null && loginStatus) || (signUpStatus != null && signUpStatus)) {
            %>
            <label>
                <span>Hello</span>
                <b><%=request.getSession().getAttribute("username")%></b>
            </label>

            <a class="btn btn-logout" href="./LoginServlet?action=logout">Logout</a>
            <%
            } else {
            %>
            <label class="text-login">Login</label>
            <button class="btn btn-signup">Sign up</button>
            <%
                }
            %>
        </div>
    </div>
</div>