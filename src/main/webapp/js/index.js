// Version: 2.0 - Fixed auto-download bug, added processing bar
// Last updated: 2025-11-11
function removeSessionUsername() {
	var xhr = new XMLHttpRequest();
	xhr.open("GET", "./LoginServlet?action=invalidate-session", true);
	xhr.send();
}

function showProcessingBar(message) {
	// Tạo thanh tiến trình nếu chưa có
	let processingBar = document.getElementById('processing-bar');
	if (!processingBar) {
		processingBar = document.createElement('div');
		processingBar.id = 'processing-bar';
		processingBar.style.cssText = `
			position: fixed;
			top: 50%;
			left: 50%;
			transform: translate(-50%, -50%);
			background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
			color: white;
			padding: 20px 40px;
			border-radius: 10px;
			box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
			z-index: 10000;
			font-size: 16px;
			text-align: center;
			min-width: 300px;
		`;
		document.body.appendChild(processingBar);
	}
	processingBar.innerHTML = `
		<div style="margin-bottom: 10px;">
			<div style="display: inline-block; width: 20px; height: 20px; border: 3px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 1s linear infinite;"></div>
		</div>
		<div>${message}</div>
	`;
	processingBar.style.display = 'block';
	
	// Thêm animation CSS nếu chưa có
	if (!document.getElementById('processing-bar-style')) {
		const style = document.createElement('style');
		style.id = 'processing-bar-style';
		style.textContent = `
			@keyframes spin {
				0% { transform: rotate(0deg); }
				100% { transform: rotate(360deg); }
			}
		`;
		document.head.appendChild(style);
	}
}

function updateProcessingBar(message) {
	const processingBar = document.getElementById('processing-bar');
	if (processingBar) {
		processingBar.innerHTML = `
			<div style="margin-bottom: 10px;">
				<div style="display: inline-block; width: 20px; height: 20px; border: 3px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 1s linear infinite;"></div>
			</div>
			<div>${message}</div>
		`;
	}
}

function hideProcessingBar() {
	const processingBar = document.getElementById('processing-bar');
	if (processingBar) {
		processingBar.style.display = 'none';
	}
}

function getAllUsernames() {
	const xhr = new XMLHttpRequest();
	xhr.open(
		"POST",
		"LoginServlet",
		true);

	// Định nghĩa sự kiện xử lý phản hồi từ controller
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			var responseData = xhr.responseText;
			console.log(responseData);
		}
	};

	const data = { 'action': 'get-users' };
	xhr.send(data);
}

document.addEventListener("DOMContentLoaded", function() {
	const loginModal = document.querySelector('.login-modal');
	const signupModal = document.querySelector('.signup-modal');

	const loginElement = document.querySelector('.text-login');
	if (loginElement !== null) {
		loginElement.addEventListener('click', function() {
			loginModal.style.display = 'flex';
		});
	}

	const btnSignUp = document.querySelector('.btn-signup');
	if (btnSignUp) {
		btnSignUp.addEventListener('click', function() {
			signupModal.style.display = 'flex';
		});
	};

	document.querySelectorAll('.close').forEach((closeElement) => {
		closeElement.addEventListener('click', function() {
			loginModal.style.display = 'none';
			signupModal.style.display = 'none';

			document.querySelectorAll('.login-modal input').forEach((element) => {
				element.value = '';
			});

			document.querySelectorAll('.signup-modal input').forEach((element) => {
				element.style.borderColor = 'black';
				element.value = '';
			});
			
			removeSessionUsername();
		})
	});

	// Lấy thẻ a và lắng nghe sự kiện click
	var uploadLink = document.getElementById("uploadLink");
	uploadLink.addEventListener("click", function(e) {
		e.preventDefault(); // Ngăn chặn hành động mặc định của thẻ a

		// Tạo input để chọn file
		const fileInput = document.createElement("input");
		fileInput.type = "file";
		fileInput.accept = ".pdf";

		// Lắng nghe sự kiện change của input file
		fileInput.addEventListener("change", function() {
			// Lấy file đã chọn
			var selectedFile = this.files[0];

			// Hiển thị thanh tiến trình
			showProcessingBar("Đang tải lên file...");

			// Gửi file lên server sử dụng XMLHttpRequest
			var xhr = new XMLHttpRequest();
			var formData = new FormData();
			formData.append("file", selectedFile);

			xhr.open(
				"POST",
				"ConverterServlet",
				true);
			// Không set responseType để nhận HTML response

			xhr.onreadystatechange = function() {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						// Cập nhật thanh tiến trình
						updateProcessingBar("File đã được thêm vào hàng đợi xử lý. Đang chờ xử lý...");
						
						// Tự động chuyển đến trang danh sách sau 2 giây
						setTimeout(function() {
							window.location.href = "./ListConvertServlet";
						}, 2000);
					} else {
						hideProcessingBar();
						alert("Lỗi: Không thể tải lên file. Vui lòng thử lại.");
						console.error("Error: " + xhr.status);
					}
				}
			};

			xhr.send(formData);
		});

		// Kích hoạt sự kiện click cho input file
		fileInput.click();
	});
});