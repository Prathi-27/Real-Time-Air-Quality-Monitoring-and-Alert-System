/**
 * JavaScript for history.html
 * Authenticates session and queries HistoryServlet for current user records.
 */

document.addEventListener('DOMContentLoaded', function () {
    verifySession();
    loadHistoryData();
});

// Verify active session via AuthCheckServlet
async function verifySession() {
    try {
        const response = await fetch('AuthCheckServlet');
        if (!response.ok) {
            window.location.href = 'login.html?session_expired=true';
            return;
        }
        const data = await response.json();
        if (data.authenticated) {
            const userGreeting = document.getElementById('userGreeting');
            if (userGreeting) {
                userGreeting.innerHTML = 'Logged in as <strong>' + escapeHtml(data.fullName || data.username) + '</strong>';
            }
        } else {
            window.location.href = 'login.html?session_expired=true';
        }
    } catch (err) {
        console.warn('Session verification issue:', err);
    }
}

// Fetch historical searches from HistoryServlet
async function loadHistoryData() {
    const tableBody = document.getElementById('historyTableBody');
    if (!tableBody) return;

    tableBody.innerHTML = `
        <tr>
            <td colspan="12" style="text-align: center; padding: 2.5rem; color: #64748b;">
                Loading your search history from database...
            </td>
        </tr>
    `;

    try {
        const res = await fetch('HistoryServlet');
        if (res.status === 401) {
            window.location.href = 'login.html?session_expired=true';
            return;
        }

        const result = await res.json();

        if (!result.data || result.data.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="12" style="text-align: center; padding: 3rem 1rem; color: #64748b;">
                        <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">🍃</div>
                        <p style="font-size: 1.15rem; font-weight: 700; color: #1e293b;">No Search History Yet</p>
                        <p style="margin-top: 0.25rem;">
                            Searches you make on the <a href="air.html" style="color: #0284c7; font-weight: 600;">Air Quality Monitor</a> will automatically be archived here.
                        </p>
                    </td>
                </tr>
            `;
            return;
        }

        let rowsHtml = '';
        result.data.forEach((row, index) => {
            rowsHtml += `
                <tr>
                    <td style="color: #94a3b8; font-weight: 600;">${index + 1}</td>
                    <td style="font-weight: 700; color: #0f172a;">${escapeHtml(row.city)}</td>
                    <td>${getAqiPill(row.aqi)}</td>
                    <td>${formatVal(row.co)}</td>
                    <td>${formatVal(row.no)}</td>
                    <td>${formatVal(row.no2)}</td>
                    <td>${formatVal(row.o3)}</td>
                    <td>${formatVal(row.so2)}</td>
                    <td style="font-weight: 700;">${formatVal(row.pm2_5)}</td>
                    <td style="font-weight: 700;">${formatVal(row.pm10)}</td>
                    <td>${formatVal(row.nh3)}</td>
                    <td style="color: #64748b; font-size: 0.85rem;">${escapeHtml(row.searched_at)}</td>
                </tr>
            `;
        });

        tableBody.innerHTML = rowsHtml;

    } catch (err) {
        console.error('History load error:', err);
        tableBody.innerHTML = `
            <tr>
                <td colspan="12" style="text-align: center; padding: 2rem; color: #dc2626;">
                    Unable to load search history. Please verify MySQL and Tomcat are active.
                </td>
            </tr>
        `;
    }
}

function getAqiPill(aqi) {
    const map = {
        1: { label: 'Good (1)', class: 'aqi-1' },
        2: { label: 'Fair (2)', class: 'aqi-2' },
        3: { label: 'Moderate (3)', class: 'aqi-3' },
        4: { label: 'Poor (4)', class: 'aqi-4' },
        5: { label: 'Very Poor (5)', class: 'aqi-5' }
    };
    const conf = map[aqi] || { label: 'AQI ' + aqi, class: 'aqi-3' };
    return `<span class="aqi-pill ${conf.class}">${conf.label}</span>`;
}

function formatVal(v) {
    if (v === null || v === undefined || isNaN(v)) return '-';
    return Number(v).toFixed(1);
}

function escapeHtml(t) {
    if (!t) return '';
    const d = document.createElement('div');
    d.textContent = t;
    return d.innerHTML;
}
