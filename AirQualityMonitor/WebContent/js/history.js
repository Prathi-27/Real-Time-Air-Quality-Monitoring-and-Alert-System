/**
 * History Page JavaScript
 * Handles session verification, fetching historical records from HistoryServlet,
 * and rendering data into the DOM table.
 */

document.addEventListener('DOMContentLoaded', function () {
    checkSession();
    loadHistory();
});

// 1. Session verification
function checkSession() {
    fetch('CheckSessionServlet')
        .then(response => {
            if (!response.ok) {
                // If not logged in, redirect to login page
                window.location.href = 'login.html?session_expired=true';
                return null;
            }
            return response.json();
        })
        .then(data => {
            if (data && data.authenticated) {
                const greeting = document.getElementById('userGreeting');
                if (greeting) {
                    greeting.innerHTML = 'Welcome, <strong>' + escapeHtml(data.fullName || data.username) + '</strong>';
                }
            }
        })
        .catch(err => {
            console.warn('Session check request error:', err);
        });
}

// 2. Fetch history records from HistoryServlet
function loadHistory() {
    const tableBody = document.getElementById('historyTableBody');
    const statusMsg = document.getElementById('statusMessage');

    if (!tableBody) return;

    tableBody.innerHTML = `
        <tr>
            <td colspan="12" style="text-align: center; padding: 2rem; color: #64748b;">
                Fetching history records from database...
            </td>
        </tr>
    `;

    fetch('HistoryServlet', {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    })
    .then(response => {
        if (response.status === 401) {
            window.location.href = 'login.html?session_expired=true';
            throw new Error('Unauthorized');
        }
        if (!response.ok) {
            throw new Error('HTTP error ' + response.status);
        }
        return response.json();
    })
    .then(result => {
        if (!result || !result.data || result.data.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="12" style="text-align: center; padding: 3rem 1rem; color: #64748b;">
                        <div style="font-size: 2rem; margin-bottom: 0.5rem;">📭</div>
                        <p style="font-size: 1.1rem; font-weight: 600; color: #334155;">No Search History Found</p>
                        <p style="font-size: 0.9rem; margin-top: 0.25rem;">
                            Searches you perform on the <a href="air.html" style="color: #2563eb;">Air Quality Monitor</a> will automatically be recorded here.
                        </p>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        result.data.forEach((row, index) => {
            const aqiBadge = getAqiBadge(row.aqi);
            rowsHtml += `
                <tr>
                    <td style="color: #94a3b8; font-weight: 500;">${index + 1}</td>
                    <td style="font-weight: 600; color: #0f172a;">${escapeHtml(row.city)}</td>
                    <td>${aqiBadge}</td>
                    <td>${formatNum(row.co)}</td>
                    <td>${formatNum(row.no)}</td>
                    <td>${formatNum(row.no2)}</td>
                    <td>${formatNum(row.o3)}</td>
                    <td>${formatNum(row.so2)}</td>
                    <td style="font-weight: 600;">${formatNum(row.pm2_5)}</td>
                    <td style="font-weight: 600;">${formatNum(row.pm10)}</td>
                    <td>${formatNum(row.nh3)}</td>
                    <td style="color: #64748b; font-size: 0.85rem;">${escapeHtml(row.searched_at)}</td>
                </tr>
            `;
        });

        tableBody.innerHTML = rowsHtml;
    })
    .catch(error => {
        if (error.message !== 'Unauthorized') {
            console.error('Failed to load history:', error);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="12" style="text-align: center; padding: 2rem; color: #dc2626;">
                        Failed to load search history from server. Please ensure MySQL and Tomcat are running.
                    </td>
                </tr>
            `;
        }
    });
}

// AQI Badge Formatter (1 = Good, 2 = Fair, 3 = Moderate, 4 = Poor, 5 = Very Poor)
function getAqiBadge(aqi) {
    const aqiMap = {
        1: { label: 'Good (1)', class: 'aqi-badge-1' },
        2: { label: 'Fair (2)', class: 'aqi-badge-2' },
        3: { label: 'Moderate (3)', class: 'aqi-badge-3' },
        4: { label: 'Poor (4)', class: 'aqi-badge-4' },
        5: { label: 'Very Poor (5)', class: 'aqi-badge-5' }
    };

    const info = aqiMap[aqi] || { label: 'AQI ' + aqi, class: 'aqi-badge-3' };
    return `<span class="aqi-badge ${info.class}">${info.label}</span>`;
}

function formatNum(val) {
    if (val === null || val === undefined || isNaN(val)) return '-';
    return Number(val).toFixed(1);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
