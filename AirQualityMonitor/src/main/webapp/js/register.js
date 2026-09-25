/**
 * JavaScript for register.html
 * Validates inputs client-side and asynchronously registers with RegistrationServlet.
 */

document.addEventListener('DOMContentLoaded', function () {
    const registerForm = document.getElementById('registerForm');
    const fullNameInput = document.getElementById('fullName');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const submitBtn = document.getElementById('submitBtn');
    const alertBox = document.getElementById('alertBox');
    const alertMsg = document.getElementById('alertMsg');

    const togglePasswordBtn1 = document.getElementById('togglePasswordBtn1');
    const togglePasswordBtn2 = document.getElementById('togglePasswordBtn2');

    function setupToggle(btn, input) {
        if (!btn || !input) return;
        btn.addEventListener('click', function () {
            const isPassword = input.getAttribute('type') === 'password';
            input.setAttribute('type', isPassword ? 'text' : 'password');
        });
    }

    setupToggle(togglePasswordBtn1, passwordInput);
    setupToggle(togglePasswordBtn2, confirmPasswordInput);

    if (registerForm) {
        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const fullName = fullNameInput.value.trim();
            const email = emailInput.value.trim();
            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;

            // Client-side validations
            if (!fullName || !email || !password || !confirmPassword) {
                showAlert('All fields are required.', 'error');
                return;
            }

            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                showAlert('Please provide a valid email address.', 'error');
                return;
            }

            if (password !== confirmPassword) {
                showAlert('Passwords do not match. Please verify.', 'error');
                return;
            }

            if (password.length < 6) {
                showAlert('Password must be at least 6 characters long.', 'error');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span>Creating Account...</span>';

            try {
                const response = await fetch('RegistrationServlet', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        full_name: fullName,
                        email: email,
                        password: password,
                        confirm_password: confirmPassword
                    })
                });

                const data = await response.json();

                if (response.ok && data.status === 'success') {
                    showAlert('Account created! Redirecting to Sign In...', 'success');
                    setTimeout(() => {
                        window.location.href = data.redirect || 'login.html?registered=true';
                    }, 800);
                } else {
                    showAlert(data.message || 'Registration failed. Please try again.', 'error');
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<span>Create Account</span>';
                }
            } catch (err) {
                console.error('Registration network error:', err);
                showAlert('Unable to reach server. Please ensure Apache Tomcat is running.', 'error');
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Create Account</span>';
            }
        });
    }

    function showAlert(message, type) {
        if (!alertBox || !alertMsg) return;
        alertMsg.textContent = message;
        alertBox.className = 'alert-box show alert-' + type;
    }
});
