<%@page import="model.BEAN.Upload"%>
<%@page import="java.util.ArrayList"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>View list convert</title>
    <link rel="stylesheet" href="./css/common.css" />
    <link rel="stylesheet" href="./css/convertion/convertion.css" />
    <%-- GIỮ NGUYÊN META REFRESH --%>
    <meta http-equiv="refresh" content="5;url=./ListConvertServlet">
    <style>
        /* ---------------------------------------------------------------------- */
        /* CSS MỚI CHO GIAO DIỆN HIỂN THỊ DANH SÁCH (THẨM MỸ CAO HƠN) */
        /* ---------------------------------------------------------------------- */

        /* ĐỊNH NGHĨA MÀU SẮC CHỦ ĐẠO */
        :root {
            --primary-start: #667eea;
            --primary-end: #764ba2;
            --primary-gradient: linear-gradient(135deg, var(--primary-start) 0%, var(--primary-end) 100%);
            --shadow-color: rgba(102, 126, 234, 0.25);
        }

        /* BACKGROUND VÀ CONTAINER CHUNG */
        body {
            background: var(--primary-gradient);
            min-height: 100vh;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            padding-bottom: 50px;
        }

        /* 🔥 BỔ SUNG: CSS CHO HEADER ĐỂ NÚT LOGOUT KHÔNG BỊ TRÀN */
        /* Giả định header.jsp chứa một thẻ <header> hoặc <div> có class là .header-nav */
        .header-nav { /* Class này cần được thêm vào thẻ ngoài cùng của header.jsp */
            display: flex;
            justify-content: space-between; /* Căn chỉnh logo bên trái, nút bên phải */
            align-items: center;
            padding: 15px 50px;
            background-color: rgba(255, 255, 255, 0.1);
            color: white;
        }

        /* Định nghĩa animation spin (giữ nguyên) */
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        /* CONTAINER CHÍNH (Card hiện đại) */
        .list-convert-container {
            max-width: 1200px;
            margin: 40px auto 20px auto;
            background: #ffffff; /* Nền trắng sạch */
            border-radius: 15px;
            padding: 40px;
            box-shadow: 0 15px 50px var(--shadow-color); /* Bóng đổ sâu hơn, rõ ràng hơn */
            border: 1px solid #f0f0f0;
        }

        /* HEADER CHỨA NÚT BACK VÀ TIÊU ĐỀ */
        .header-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 1px solid #e0e0e0;
        }

        /* TIÊU ĐỀ LỚN VỚI HIỆU ỨNG GRADIENT TEXT */
        .header-text {
            font-size: 2.2em !important;
            font-weight: 800;
            /* Áp dụng gradient cho chữ */
            background: var(--primary-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            text-align: center;
            margin: 0;
        }

        /* NÚT BACK TO HOME */
        .btn-back {
            text-decoration: none;
            background: var(--primary-gradient); /* Dùng gradient cho nút */
            color: white !important;
            padding: 10px 25px;
            border-radius: 30px;
            font-weight: 600;
            transition: all 0.3s;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
        }
        .btn-back:hover {
            transform: translateY(-3px);
            opacity: 0.9;
        }

        /* STYLING CHO BẢNG (Tối giản) */
        .styled-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.9em;
            border-radius: 10px;
            overflow: hidden;
        }

        .styled-table thead tr {
            background-color: #f7f7ff; /* Nền header nhẹ */
            color: #333; /* Màu chữ đậm */
            text-align: center;
            font-weight: 700;
            border-bottom: 2px solid var(--primary-start);
        }

        .styled-table th, .styled-table td {
            padding: 18px 15px; /* Tăng padding */
            text-align: center;
            border: none; /* Loại bỏ viền cell */
        }
        .styled-table td {
            border-bottom: 1px solid #f0f0f0; /* Chỉ giữ viền dưới mỏng */
        }

        /* Màu xen kẽ cho hàng (Alternating row colors) */
        .styled-table tbody tr:nth-of-type(odd) {
            background-color: #ffffff;
        }
        .styled-table tbody tr:nth-of-type(even) {
            background-color: #f9f9f9; /* Màu nền nhẹ nhàng hơn */
        }
        .styled-table tbody tr:hover {
            background-color: #eef2ff; /* Màu hover liên quan đến màu chủ đạo */
            transition: background-color 0.2s;
        }

        /* DOWNLOAD LINK COLOR */
        .styled-table td a {
            color: var(--primary-start);
            text-decoration: none;
            font-weight: 600;
            transition: color 0.2s;
        }
        .styled-table td a:hover {
            text-decoration: underline;
            color: var(--primary-end);
        }

        /* STYLING CHO STATUS BADGES (Pill-shaped) */
        .status-badge {
            display: inline-flex;
            align-items: center;
            font-size: 0.85em;
            font-weight: 600;
            padding: 6px 12px;
            border-radius: 50px; /* Hình viên thuốc */
            white-space: nowrap;
        }

        /* Ẩn spinner cũ để áp dụng style mới */
        .processing-spinner {
            margin-right: 5px;
        }

        /* THÔNG BÁO KHÔNG CÓ DỮ LIỆU */
        .no-data-message {
            text-align: center;
            padding: 50px;
            font-size: 1.3em;
            color: #777;
            background-color: #f9f9f9;
            border-radius: 10px;
            border: 1px dashed #ccc;
        }
    </style>
</head>
<body>
<%-- GIỮ NGUYÊN HEADER --%>
<%@include file="header.jsp"%>
<%
    ArrayList<Upload> uploads = (ArrayList<Upload>) request.getSession().getAttribute("uploads");
%>

<%-- CONTAINER CHÍNH MỚI --%>
<div class="list-convert-container">
    <div class="table-container scrollbar">

        <div class="header-content">
            <%-- NÚT BACK: GIỮ NGUYÊN CHỨC NĂNG --%>
            <a class="btn btn-back" href="./index.jsp">Back to home</a>

            <%-- TIÊU ĐỀ: GIỮ NGUYÊN CHỮ, ÁP DỤNG STYLE MỚI --%>
            <h1 class="text-center header-text fs-20px">List file converted</h1>
        </div>

        <%
            if (uploads != null && uploads.size() > 0) {
        %>
        <table class="styled-table">
            <thead class="thead-dark">
            <tr>
                <th class="text-center">No</th>
                <th class="text-center">File upload</th>
                <th class="text-center">File converted</th>
                <th class="text-center">Status</th>
                <th class="text-center">Date</th>
            </tr>
            </thead>
            <tbody>
            <%
                int i = 1;
                for (Upload upload : uploads) {
            %>
            <tr class="active-row">
                <td class="text-center"><%=i%></td>
                <td class="text-center"><%=upload.getFileNameUpload()%></td>
                <td class="text-center">
                    <%
                        String status = upload.getStatus();
                        if ("completed".equals(status)) {
                    %>
                    <%-- DOWNLOAD LINK: GIỮ NGUYÊN CHỨC NĂNG --%>
                    <a href="./DownloadFileServlet?action=downloadfile&fileName=<%=java.net.URLEncoder.encode(upload.getFileNameOutputInServer(), "UTF-8")%>"
                       target="_blank"><%=upload.getFileNameOutput()%></a>
                    <%
                    } else {
                    %>
                    <%=upload.getFileNameOutput()%>
                    <%
                        }
                    %>
                </td>
                <td class="text-center">
                    <%
                        // STATUS BADGES: GIỮ NGUYÊN LOGIC, ÁP DỤNG STYLE BADGE MỚI
                        if ("queued".equals(status)) {
                    %>
                    <span class="status-badge" style="color: #FF9800; border: 1px solid #FF9800; background-color: #FFF8E1;">
                  <span class="processing-spinner" style="display: inline-block; width: 12px; height: 12px; border: 2px solid rgba(255,165,0,0.3); border-top-color: #FF9800; border-radius: 50%; animation: spin 1s linear infinite;"></span>
                  Đang thực hiện (queued)
                </span>
                    <%
                    } else if ("processing".equals(status)) {
                    %>
                    <span class="status-badge" style="color: #2196F3; border: 1px solid #2196F3; background-color: #E3F2FD;">
                  <span class="processing-spinner" style="display: inline-block; width: 12px; height: 12px; border: 2px solid rgba(0,0,255,0.3); border-top-color: #2196F3; border-radius: 50%; animation: spin 1s linear infinite;"></span>
                  Đang thực hiện (processing)
                </span>
                    <%
                    } else if ("completed".equals(status)) {
                    %>
                    <span class="status-badge" style="color: #4CAF50; border: 1px solid #4CAF50; background-color: #E8F5E9;">✓ Hoàn thành</span>
                    <%
                    } else if ("failed".equals(status)) {
                    %>
                    <span class="status-badge" style="color: #F44336; border: 1px solid #F44336; background-color: #FFEBEE;">✗ Thất bại</span>
                    <%
                    } else {
                    %>
                    <span><%=status%></span>
                    <%
                        }
                    %>
                </td>
                <td class="text-center"><%=upload.getDate()%></td>
            </tr>
            <%
                    i++;
                }
            %>
            </tbody>
        </table>
        <%
        } else {
        %>
        <%-- THAY THẾ BẰNG THÔNG BÁO ĐẸP HƠN --%>
        <h3 class="no-data-message">Không có tài liệu nào được chuyển đổi trước đây. Hãy quay lại trang chủ để bắt đầu!</h3>
        <%
            }
        %>
    </div>
</div>
</body>
</html>