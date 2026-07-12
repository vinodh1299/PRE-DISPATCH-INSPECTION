<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Registration</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f9;
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            box-sizing: border-box;
        }

        .container {
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            padding: 20px;
            max-width: 400px;
            width: 100%;
            box-sizing: border-box;
            margin-top: 10%;
        }

        h2 {
            text-align: center;
            color: #333;
        }

        #feedback {
            color: green;
            font-weight: bold;
            text-align: center;
            margin-bottom: 10px;
        }

        .error {
            color: red;
        }

        form {
            display: flex;
            flex-direction: column;
        }

        label {
            margin: 7px 0 5px;
            color: #333;
        }

        input[type="text"],
        input[type="number"],
        input[type="password"] {
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            margin-bottom: 10px;
            box-sizing: border-box;
            width: 100%;
        }

        button {
            padding: 10px;
            border: none;
            border-radius: 4px;
            background-color: #007bff;
            color: white;
            font-size: 16px;
            cursor: pointer;
            margin-bottom: 10px;
        }

        button:disabled {
            background-color: gray;
            cursor: not-allowed;
        }

        @media (max-width: 480px) {
            .container {
                padding: 15px;
            }

            input[type="text"],
            input[type="number"],
            input[type="password"] {
                padding: 8px;
            }

            button {
                padding: 8px;
                font-size: 14px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h2>Register</h2>
        <div id="feedback"></div>
        <form id="registration-form" action="registerServlet" method="post">
            <label for="emp-name">EMP NAME:</label>
            <input type="text" id="username" name="username" required>
            
                        <label for="emp-no">EMP NO:</label>
            <input type="text" id="empNo" name="empNo" required>
                        
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" required>
            
            <button type="submit" id="submit-btn">Register</button>
        </form>
    </div>
</body>
</html>
