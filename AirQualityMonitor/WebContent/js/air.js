/**
 * Air Quality Monitor JavaScript
 * Retains 100% of existing OpenWeatherMap flow (Geocoding -> Air Pollution -> Display).
 * ADDED MINIMUM INTEGRATIONS:
 * 1. checkSession() on page load (redirects to login.html if not authenticated)
 * 2. saveAirQualityData(...) via fetch to SaveAirQualityServlet after successful API fetch
 */

// Default OpenWeatherMap API Key (Users can replace with their own in the input or code)
const DEFAULT_API_KEY = "bd5e378503939ddaee76f12ad7a97608"; // Standard active test key

// =========================================================================
// [ADDED CODE 1/2]: Authentication Check on Page Load
// =========================================================================
document.addEventListener('DOMContentLoaded', function () {
    checkSession();

    // Allow pressing "Enter" key in city search input
    const cityInput = document.getElementById('cityInput');
    if (cityInput) {
        cityInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                searchAirQuality();
            }
        });
    }
});

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
                const userGreeting = document.getElementById('userGreeting');
                if (userGreeting) {
                    userGreeting.innerHTML = 'Welcome, <strong>' + escapeHtml(data.fullName || data.username) + '</strong>';
                }
            }
        })
        .catch(err => {
            console.warn('Session check warning:', err);
        });
}

// =========================================================================
// EXISTING OPENWEATHERMAP CODE (Preserved Exactly)
// =========================================================================
async function searchAirQuality() {
    const cityInput = document.getElementById('cityInput');
    const loadingMessage = document.getElementById('loadingMessage');
    const errorMessage = document.getElementById('errorMessage');
    const resultContainer = document.getElementById('resultContainer');
    const saveStatus = document.getElementById('saveStatus');

    const cityName = cityInput.value.trim();
    if (!cityName) {
        showError("Please enter a city name.");
        return;
    }

    // Reset UI states
    errorMessage.style.display = 'none';
    if (saveStatus) saveStatus.style.display = 'none';
    loadingMessage.style.display = 'block';
    resultContainer.style.display = 'none';

    const customKeyInput = document.getElementById('customApiKey');
    const apiKey = (customKeyInput && customKeyInput.value.trim()) ? customKeyInput.value.trim() : DEFAULT_API_KEY;

    try {
        // Step 1: Geocoding API to fetch coordinates (latitude & longitude)
        const geoUrl = `https://api.openweathermap.org/geo/1.0/direct?q=${encodeURIComponent(cityName)}&limit=1&appid=${apiKey}`;
        const geoResponse = await fetch(geoUrl);

        if (!geoResponse.ok) {
            throw new Error(`Geocoding error (${geoResponse.status}). Please check city name or API key.`);
        }

        const geoData = await geoResponse.json();
        if (!geoData || geoData.length === 0) {
            throw new Error(`City "${cityName}" not found. Please verify spelling.`);
        }

        const location = geoData[0];
        const lat = location.lat;
        const lon = location.lon;
        const resolvedCity = `${location.name}${location.country ? ', ' + location.country : ''}`;

        // Step 2: Air Pollution API using coordinates
        const pollutionUrl = `https://api.openweathermap.org/data/2.5/air_pollution?lat=${lat}&lon=${lon}&appid=${apiKey}`;
        const pollutionResponse = await fetch(pollutionUrl);

        if (!pollutionResponse.ok) {
            throw new Error(`Air pollution data error (${pollutionResponse.status}).`);
        }

        const pollutionData = await pollutionResponse.json();
        if (!pollutionData || !pollutionData.list || pollutionData.list.length === 0) {
            throw new Error("No air quality measurements available for this location.");
        }

        const current = pollutionData.list[0];
        const aqi = current.main.aqi; // 1 = Good, 2 = Fair, 3 = Moderate, 4 = Poor, 5 = Very Poor
        const components = current.components;

        // Step 3: Render Existing UI Elements (AQI, Pollutants, Health Alerts)
        displayResults(resolvedCity, lat, lon, aqi, components);
        loadingMessage.style.display = 'none';
        resultContainer.style.display = 'block';

        // =========================================================================
        // [ADDED CODE 2/2]: Save successful air quality data to MySQL via Servlet
        // (Only executed if OpenWeatherMap succeeds!)
        // =========================================================================
        saveAirQualityData(resolvedCity, lat, lon, aqi, components);

    } catch (err) {
        loadingMessage.style.display = 'none';
        showError(err.message || "Failed to retrieve air quality data.");
        // Note: If OpenWeatherMap fails, do NOT save data to MySQL.
    }
}

function displayResults(cityName, lat, lon, aqi, comp) {
    document.getElementById('cityNameDisplay').textContent = cityName;
    document.getElementById('coordinatesDisplay').textContent = `Latitude: ${lat.toFixed(4)}, Longitude: ${lon.toFixed(4)}`;

    // AQI Config & Health Advisory
    const aqiMeta = {
        1: { label: "1 - Good", class: "aqi-1", badgeClass: "aqi-badge-1", alert: "Air quality is considered satisfactory, and air pollution poses little or no risk.", rec: "Great day for outdoor activities, walking, and exercising in the fresh air." },
        2: { label: "2 - Fair", class: "aqi-2", badgeClass: "aqi-badge-2", alert: "Air quality is acceptable; however, some pollutants may pose a moderate concern for sensitive individuals.", rec: "Sensitive individuals should consider reducing prolonged or heavy outdoor exertion." },
        3: { label: "3 - Moderate", class: "aqi-3", badgeClass: "aqi-badge-3", alert: "Members of sensitive groups may experience health effects. The general public is less likely to be affected.", rec: "People with respiratory or heart diseases, children, and older adults should limit prolonged outdoor exertion." },
        4: { label: "4 - Poor", class: "aqi-4", badgeClass: "aqi-badge-4", alert: "Everyone may begin to experience health effects; members of sensitive groups may experience more serious health effects.", rec: "Avoid prolonged outdoor activities. Consider wearing an N95 mask outdoors and run an air purifier indoors." },
        5: { label: "5 - Very Poor", class: "aqi-5", badgeClass: "aqi-badge-5", alert: "Health warnings of emergency conditions. The entire population is more likely to be affected.", rec: "Remain indoors with windows closed. Keep physical exertion to a minimum and seek medical advice if experiencing difficulty breathing." }
    };

    const info = aqiMeta[aqi] || aqiMeta[3];
    const aqiCard = document.getElementById('aqiCard');
    const aqiBadge = document.getElementById('aqiBadge');

    aqiCard.className = `card aqi-hero-card ${info.class}`;
    aqiBadge.className = `aqi-badge ${info.badgeClass}`;
    aqiBadge.textContent = info.label;

    document.getElementById('healthAlert').textContent = info.alert;
    document.getElementById('healthRecommendations').textContent = info.rec;

    // Display all 8 pollutant components
    document.getElementById('val_pm2_5').textContent = comp.pm2_5 !== undefined ? comp.pm2_5.toFixed(1) : '-';
    document.getElementById('val_pm10').textContent = comp.pm10 !== undefined ? comp.pm10.toFixed(1) : '-';
    document.getElementById('val_no2').textContent = comp.no2 !== undefined ? comp.no2.toFixed(1) : '-';
    document.getElementById('val_so2').textContent = comp.so2 !== undefined ? comp.so2.toFixed(1) : '-';
    document.getElementById('val_co').textContent = comp.co !== undefined ? comp.co.toFixed(1) : '-';
    document.getElementById('val_o3').textContent = comp.o3 !== undefined ? comp.o3.toFixed(1) : '-';
    document.getElementById('val_no').textContent = comp.no !== undefined ? comp.no.toFixed(1) : '-';
    document.getElementById('val_nh3').textContent = comp.nh3 !== undefined ? comp.nh3.toFixed(1) : '-';
}

function showError(msg) {
    const errorBox = document.getElementById('errorMessage');
    errorBox.textContent = msg;
    errorBox.style.display = 'block';
}

// =========================================================================
// [ADDED CODE]: Send Air Quality Record to SaveAirQualityServlet
// =========================================================================
function saveAirQualityData(city, lat, lon, aqi, comp) {
    const saveStatus = document.getElementById('saveStatus');

    // Use URLSearchParams for seamless Java Servlet request.getParameter compatibility
    const params = new URLSearchParams();
    params.append('city', city);
    params.append('latitude', lat);
    params.append('longitude', lon);
    params.append('aqi', aqi);
    params.append('co', comp.co !== undefined ? comp.co : 0);
    params.append('no', comp.no !== undefined ? comp.no : 0);
    params.append('no2', comp.no2 !== undefined ? comp.no2 : 0);
    params.append('o3', comp.o3 !== undefined ? comp.o3 : 0);
    params.append('so2', comp.so2 !== undefined ? comp.so2 : 0);
    params.append('pm2_5', comp.pm2_5 !== undefined ? comp.pm2_5 : 0);
    params.append('pm10', comp.pm10 !== undefined ? comp.pm10 : 0);
    params.append('nh3', comp.nh3 !== undefined ? comp.nh3 : 0);

    fetch('SaveAirQualityServlet', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body: params.toString()
    })
    .then(res => {
        if (res.status === 401) {
            console.warn("Session expired while saving search. Redirecting to login...");
            window.location.href = 'login.html?session_expired=true';
            return null;
        }
        return res.json();
    })
    .then(data => {
        if (data && data.status === 'success') {
            if (saveStatus) {
                saveStatus.innerHTML = '<span class="save-status-toast">💾 Search saved to history</span>';
                saveStatus.style.display = 'block';
            }
        }
    })
    .catch(err => {
        console.warn('Note: Could not reach SaveAirQualityServlet (check Tomcat & MySQL):', err);
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
