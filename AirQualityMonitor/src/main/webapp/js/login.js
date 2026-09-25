/**
 * JavaScript for login.html
 * Handles password visibility toggle, URL feedback messages,
 * and asynchronous form submission to LoginServlet.
 */

document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('togglePasswordBtn');
    const submitBtn = document.getElementById('submitBtn');
    const alertBox = document.getElementById('alertBox');
    const alertMsg = document.getElementById('alertMsg');

    // 1. Password Visibility Toggle
    if (togglePasswordBtn) {
        togglePasswordBtn.addEventListener('click', function () {
            const isPassword = passwordInput.getAttribute('type') === 'password';
            passwordInput.setAttribute('type', isPassword ? 'text' : 'password');

            if (isPassword) {
                togglePasswordBtn.innerHTML = `
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/>
                        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/>
                        <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/>
                        <line x1="2" x2="22" y1="2" y2="22"/>
                    </svg>
                `;
            } else {
                togglePasswordBtn.innerHTML = `
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
                        <circle cx="12" cy="12" r="3"/>
                    </svg>
                `;
            }
        });
    }

    // 2. Parse URL parameters for feedback
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('registered')) {
        showAlert('Registration successful! Please sign in with your credentials.', 'success');
    } else if (urlParams.has('logged_out')) {
        showAlert('You have been logged out securely.', 'info');
    } else if (urlParams.has('session_expired')) {
        showAlert('Your session has expired. Please sign in again.', 'error');
    }

    // 3. Form Submission via fetch() to LoginServlet
    if (loginForm) {
        loginForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!email || !password) {
                showAlert('Please enter both email and password.', 'error');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span>Signing in...</span>';

            try {
                const response = await fetch('LoginServlet', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                });

                const data = await response.json();

                if (response.ok && data.status === 'success') {
                    showAlert('Signed in successfully! Redirecting to Air Quality Monitor...', 'success');
                    setTimeout(() => {
                        window.location.href = data.redirect || 'air.html';
                    }, 500);
                } else {
                    // Exact failure message display: "Invalid email or password."
                    showAlert(data.message || 'Invalid email or password.', 'error');
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = `
                        <span>Sign In</span>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M5 12h14"/>
                            <path d="m12 5 7 7-7 7"/>
                        </svg>
                    `;
                }
            } catch (err) {
                console.error('Login network error:', err);
                showAlert('Unable to reach server. Please ensure Apache Tomcat is running on port 8080.', 'error');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Sign In</span>';
            }
        });
    }

    function showAlert(message, type) {
        if (!alertBox || !alertMsg) return;
        alertMsg.textContent = message;
        alertBox.className = 'alert-box show alert-' + type;
    }
});
