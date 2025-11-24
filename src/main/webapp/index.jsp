<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>PDF Conversion</title>
    <link rel="stylesheet" href="./css/common.css" />
    <link rel="stylesheet" href="./css/convertion/convertion.css" />
    <link rel="stylesheet" type="text/css" href="css/login/login.css">

    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow-x: hidden;
        }

        .main-content {
            display: flex;
            align-items: center;
            justify-content: space-around;
            min-height: calc(100vh - 80px);
            padding: 40px 100px;
        }

        .feature-list {
            color: white;
            max-width: 400px;
        }

        .feature-list h2 { font-size: 2.5em; margin-bottom: 20px; }
        .feature-list ul { list-style: none; }
        .feature-list ul li { margin-bottom: 15px; font-size: 1.1em; display: flex; align-items: center; }
        .feature-list ul li span { margin-right: 15px; color: #ffd89b; font-size: 1.5em; }

        .conversion-container {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 20px;
            padding: 50px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            max-width: 450px;
            width: 90%;
            text-align: center;
        }

        .icon-wrapper { font-size: 4em; margin-bottom: 20px; color: #667eea; }

        .content-title {
            font-size: 2.5em;
            font-weight: 700;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .content-subtitle { color: #666; font-size: 1.1em; margin-bottom: 40px; }

        .content-uploader { margin-bottom: 20px; }

        .btn-upload {
            padding: 18px 50px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 50px;
            cursor: pointer;
            font-size: 1.1em;
            border: none;
            text-decoration: none;
            display: inline-block;
        }

        .modal { display: none; }
    </style>
</head>
<body>

<%@include file="header.jsp"%>

<div class="main-content">

    <div class="feature-list">
        <h2>Chuyển đổi PDF, đơn giản và an toàn</h2>
        <ul>
            <li><span>✅</span>Tốc độ chuyển đổi siêu nhanh.</li>
            <li><span>🔒</span>Bảo mật dữ liệu tuyệt đối.</li>
            <li><span>💡</span>Không cần cài phần mềm.</li>
        </ul>
    </div>

    <div class="conversion-container">
        <div class="icon-wrapper">📄➜📝</div>

        <h1 class="content-title">PDF to WORD Conversion</h1>
        <h2 class="content-subtitle">
            Converting pdf documents to word is very convenient
        </h2>

        <!-- KIỂM TRA LOGIN -->
        <%
        loginStatus = (Boolean) session.getAttribute("login-status");
        signUpStatus = (Boolean) session.getAttribute("signup-status");

        boolean loggedIn =
                (loginStatus != null && loginStatus)
                        || (signUpStatus != null && signUpStatus);
    %>


        <% if (loggedIn) { %>

        <!-- Nút chọn file -->
        <div class="content-uploader">
            <a href="#!" class="btn-upload" id="uploadLink">Choose PDF file</a>
        </div>

        <!-- Nút xem danh sách file đã convert -->
        <div class="content-uploader">
            <a href="./ListConvertServlet" class="btn-upload">View list converted</a>
        </div>

        <% } else { %>

        <!-- Nếu chưa login -->
        <div class="content-uploader">
            <a href="#!" class="btn-upload" onclick="openLoginModal()">Login to continue</a>
        </div>

        <% } %>

    </div>
</div>

<%@include file="login-modal.jsp"%>
<%@include file="signup-modal.jsp"%>

<script src="./js/index.js?v=2.0"></script>
<script src="./js/signup.js"></script>

<script>
    // CHỐNG lỗi khi loginStatus = null
    var loginStatus = <%= (loginStatus == null ? "null" : loginStatus.toString()) %>;

    if (loginStatus !== null && loginStatus === false) {
        document.querySelector('.login-modal').style.display = 'flex';
    }

    function openLoginModal() {
        document.querySelector('.login-modal').style.display = 'flex';
    }
</script>

</body>
</html>
