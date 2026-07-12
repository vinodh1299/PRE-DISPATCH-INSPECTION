<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Login</title>
    <style>
        /* --- Cool & Professional Animated Dark Theme --- */
        body {
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
            background: radial-gradient(circle at 50% 50%, #0f172a 0%, #020617 100%);
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            overflow: hidden;
            position: relative;
        }
        

        /* Subtle moving glow effects in background */
        body::before, body::after {
            content: "";
            position: absolute;
            width: 350px;
            height: 350px;
            border-radius: 50%;
            filter: blur(120px);
            z-index: 1;
            opacity: 0.12;
            animation: floatGlow 12s ease-in-out infinite alternate;
        }

        body::before {
            background: #1A9FD8;
            top: 15%;
            left: 20%;
        }

        body::after {
            background: #002060;
            bottom: 15%;
            right: 20%;
            animation-delay: -6s;
        }

        @keyframes floatGlow {
            0% { transform: translateY(0) scale(1); }
            100% { transform: translateY(40px) scale(1.15); }
        }

        form {
            background: rgba(15, 23, 42, 0.75);
            border: 1px solid rgba(255, 255, 255, 0.08);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            padding: 40px 30px;
            border-radius: 16px;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
            width: 360px;
            z-index: 10;
            animation: cardEntrance 0.7s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            transform: translateY(20px);
            opacity: 0;
            box-sizing: border-box;
        }

        @keyframes cardEntrance {
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }

        form h2 {
            margin: 0 0 30px 0;
            color: #fff;
            font-size: 26px;
            font-weight: 700;
            text-align: center;
            letter-spacing: 0.5px;
            position: relative;
        }

        form h2::after {
            content: "";
            position: absolute;
            width: 45px;
            height: 3px;
            background: linear-gradient(90deg, #1A9FD8, #002060);
            bottom: -8px;
            left: 50%;
            transform: translateX(-50%);
            border-radius: 2px;
        }

        #login-container {
            background: transparent !important;
            border: none !important;
            box-shadow: none !important;
            padding: 0 !important;
            margin: 0 !important;
            width: 100% !important;
        }

        label {
            display: block;
            color: #94a3b8;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 6px;
            margin-top: 18px;
            letter-spacing: 0.5px;
        }

        input[type="text"],
        input[type="password"] {
            width: 100%;
            padding: 12px 16px;
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 8px;
            color: #fff;
            font-size: 14px;
            outline: none;
            box-sizing: border-box;
            transition: all 0.25s ease;
        }

        input[type="text"]:focus,
        input[type="password"]:focus {
            border-color: #1A9FD8;
            background: rgba(255, 255, 255, 0.08);
            box-shadow: 0 0 0 3px rgba(26, 159, 216, 0.25);
        }

        .submit button {
            background: linear-gradient(135deg, #1A9FD8 0%, #002060 100%);
            color: white;
            padding: 12px 20px;
            margin-top: 28px;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            width: 100%;
            font-size: 15px;
            font-weight: 700;
            transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 15px rgba(26, 159, 216, 0.2);
            outline: none;
        }

        .submit button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(26, 159, 216, 0.4);
            filter: brightness(1.15);
        }

        .submit button:active {
            transform: translateY(0);
        }

        .error {
            background: rgba(239, 68, 68, 0.12);
            border: 1px solid rgba(239, 68, 68, 0.25);
            color: #f87171;
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 13px;
            text-align: center;
            margin-bottom: 18px;
        }

        /* Link/footer styling */
        form div p {
            text-align: center;
            color: #64748b;
            font-weight: 600 !important;
            margin-top: 25px;
            margin-bottom: 0;
            margin-left: 0 !important;
        }

        form div p a {
            color: #1A9FD8 !important;
            font-weight: 700;
            transition: color 0.25s ease;
        }

        form div p a:hover {
            color: #24e024 !important;
            text-decoration: underline !important;
        }
    </style>
</head>
<body>
    <form id="loginForm" method="post" action="loginServlet">
        <h2>YE PDI INSPECTION LOGIN</h2>

        <!-- Error message section -->
        <c:if test="${not empty errorMessage}">
            <div class="error">${errorMessage}</div>
        </c:if>

        <!-- Username and password login section -->
        <div id="login-container">
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" required>
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" required>
            <div class="submit">
                <button type="submit" id="login-btn">Login</button>
            </div>
        </div>

        <div>
            <p style="font-size: 13px; margin-left: 18px; font-weight: 700;">
                If you don't have an account, 
                <a style="text-decoration: none; color: #24e024;" href="register.jsp">Create Here</a>.
            </p>
        </div>
    </form>
</body>
</html>
