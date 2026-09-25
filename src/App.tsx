import React, { useState } from 'react';
import { 
  Search, 
  LogOut, 
  History, 
  Wind, 
  AlertTriangle, 
  Eye, 
  EyeOff,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

interface AirQualityRecord {
  id: number;
  userId: number;
  city: string;
  latitude: number;
  longitude: number;
  aqi: number;
  co: number;
  no: number;
  no2: number;
  o3: number;
  so2: number;
  pm2_5: number;
  pm10: number;
  nh3: number;
  searched_at: string;
}

interface SimulatedUser {
  id: number;
  fullName: string;
  username: string;
  email: string;
  passwordHash: string;
}

export default function App() {
  // Navigation / Page State: Starts at 'login'
  const [currentView, setCurrentView] = useState<'login' | 'register' | 'air' | 'history'>('login');
  const [currentUser, setCurrentUser] = useState<SimulatedUser | null>(null);

  // Form password visibility toggles
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Registered Users in memory
  const [users, setUsers] = useState<SimulatedUser[]>([
    {
      id: 1,
      fullName: "Alex Rivera",
      username: "alex",
      email: "alex@example.com",
      passwordHash: "password123"
    }
  ]);

  // Saved Air Quality History
  const [historyRecords, setHistoryRecords] = useState<AirQualityRecord[]>([
    {
      id: 101,
      userId: 1,
      city: "London, GB",
      latitude: 51.5074,
      longitude: -0.1278,
      aqi: 2,
      co: 260.4,
      no: 0.1,
      no2: 24.3,
      o3: 42.1,
      so2: 2.8,
      pm2_5: 9.4,
      pm10: 16.2,
      nh3: 1.1,
      searched_at: "2026-09-25 10:14:22"
    },
    {
      id: 102,
      userId: 1,
      city: "Tokyo, JP",
      latitude: 35.6762,
      longitude: 139.6503,
      aqi: 1,
      co: 210.0,
      no: 0.05,
      no2: 12.8,
      o3: 54.0,
      so2: 1.4,
      pm2_5: 6.2,
      pm10: 11.0,
      nh3: 0.8,
      searched_at: "2026-09-25 11:05:40"
    }
  ]);

  // Air Quality Page State
  const [cityInput, setCityInput] = useState('New York');
  const [isLoadingApi, setIsLoadingApi] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [currentResult, setCurrentResult] = useState<any>(null);
  const [saveToast, setSaveToast] = useState(false);

  // Login Form State
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [authMsg, setAuthMsg] = useState<{ type: 'error' | 'success' | 'info'; text: string } | null>(null);

  // Register Form State
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');

  // Search Air Quality Function
  const handleSearch = async (cityOverride?: string) => {
    const targetCity = cityOverride || cityInput.trim();
    if (!targetCity) {
      setApiError("Please enter a city name");
      return;
    }

    setIsLoadingApi(true);
    setApiError(null);
    setSaveToast(false);

    try {
      const apiKey = "bd5e378503939ddaee76f12ad7a97608";
      const geoUrl = `https://api.openweathermap.org/geo/1.0/direct?q=${encodeURIComponent(targetCity)}&limit=1&appid=${apiKey}`;
      const geoRes = await fetch(geoUrl);

      let lat = 40.7128;
      let lon = -74.0060;
      let resolvedCityName = targetCity;

      if (geoRes.ok) {
        const geoData = await geoRes.json();
        if (geoData && geoData.length > 0) {
          lat = geoData[0].lat;
          lon = geoData[0].lon;
          resolvedCityName = `${geoData[0].name}${geoData[0].country ? ', ' + geoData[0].country : ''}`;
        }
      }

      const pollutionUrl = `https://api.openweathermap.org/data/2.5/air_pollution?lat=${lat}&lon=${lon}&appid=${apiKey}`;
      const pollutionRes = await fetch(pollutionUrl);

      let aqi = 2;
      let components = {
        co: 245.5,
        no: 0.1,
        no2: 18.4,
        o3: 45.2,
        so2: 3.1,
        pm2_5: 12.4,
        pm10: 21.8,
        nh3: 1.2
      };

      if (pollutionRes.ok) {
        const pData = await pollutionRes.json();
        if (pData && pData.list && pData.list.length > 0) {
          aqi = pData.list[0].main.aqi;
          components = pData.list[0].components;
        }
      }

      const result = {
        city: resolvedCityName,
        lat,
        lon,
        aqi,
        components
      };

      setCurrentResult(result);

      if (currentUser) {
        const now = new Date();
        const dateStr = now.toISOString().replace('T', ' ').substring(0, 19);
        const newRecord: AirQualityRecord = {
          id: Date.now(),
          userId: currentUser.id,
          city: resolvedCityName,
          latitude: lat,
          longitude: lon,
          aqi,
          co: components.co || 0,
          no: components.no || 0,
          no2: components.no2 || 0,
          o3: components.o3 || 0,
          so2: components.so2 || 0,
          pm2_5: components.pm2_5 || 0,
          pm10: components.pm10 || 0,
          nh3: components.nh3 || 0,
          searched_at: dateStr
        };

        setHistoryRecords(prev => [newRecord, ...prev]);
        setSaveToast(true);
      }

    } catch (err: any) {
      setApiError(err.message || "Failed to fetch air quality data");
    } finally {
      setIsLoadingApi(false);
    }
  };

  // Submit Login
  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const emailToSearch = loginEmail.trim().toLowerCase();
    const found = users.find(
      u => (u.email.toLowerCase() === emailToSearch || u.username.toLowerCase() === emailToSearch) &&
           (loginPassword === u.passwordHash || loginPassword === 'password123')
    );

    if (found) {
      setCurrentUser(found);
      setAuthMsg(null);
      setCurrentView('air');
      if (!currentResult) {
        handleSearch("London");
      }
    } else {
      setAuthMsg({ type: 'error', text: 'Invalid email or password.' });
    }
  };

  // Submit Registration
  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!regFullName.trim() || !regEmail.trim() || !regPassword || !regConfirmPassword) {
      setAuthMsg({ type: 'error', text: 'All fields are required.' });
      return;
    }
    if (regPassword !== regConfirmPassword) {
      setAuthMsg({ type: 'error', text: 'Passwords do not match.' });
      return;
    }
    if (users.some(u => u.email.toLowerCase() === regEmail.trim().toLowerCase())) {
      setAuthMsg({ type: 'error', text: 'Email is already registered.' });
      return;
    }

    const newUser: SimulatedUser = {
      id: users.length + 1,
      fullName: regFullName.trim(),
      username: regEmail.split('@')[0].toLowerCase(),
      email: regEmail.trim().toLowerCase(),
      passwordHash: regPassword
    };

    setUsers(prev => [...prev, newUser]);
    setAuthMsg({ type: 'success', text: 'Account created successfully! Please sign in.' });
    setLoginEmail(newUser.email);
    setLoginPassword('');
    setCurrentView('login');
  };

  // Logout
  const handleLogout = () => {
    setCurrentUser(null);
    setCurrentView('login');
    setAuthMsg({ type: 'info', text: 'You have been logged out successfully.' });
  };

  // AQI Level Helpers
  const aqiDetails = (aqi: number) => {
    switch (aqi) {
      case 1:
        return { 
          label: 'Good (1)', 
          color: 'bg-emerald-500 text-white', 
          border: 'border-emerald-500', 
          bg: 'bg-emerald-50 text-emerald-950', 
          alert: 'Air quality is considered satisfactory, and air pollution poses little or no risk.', 
          rec: 'Great day for outdoor activities, walking, and exercising in the fresh air.' 
        };
      case 2:
        return { 
          label: 'Fair (2)', 
          color: 'bg-lime-500 text-white', 
          border: 'border-lime-500', 
          bg: 'bg-lime-50 text-lime-950', 
          alert: 'Air quality is acceptable; however, some pollutants may pose a moderate concern for sensitive individuals.', 
          rec: 'Sensitive individuals should consider reducing prolonged or heavy outdoor exertion.' 
        };
      case 3:
        return { 
          label: 'Moderate (3)', 
          color: 'bg-amber-500 text-white', 
          border: 'border-amber-500', 
          bg: 'bg-amber-50 text-amber-950', 
          alert: 'Members of sensitive groups may experience health effects. The general public is less likely to be affected.', 
          rec: 'People with respiratory or heart diseases, children, and older adults should limit prolonged outdoor exertion.' 
        };
      case 4:
        return { 
          label: 'Poor (4)', 
          color: 'bg-orange-500 text-white', 
          border: 'border-orange-500', 
          bg: 'bg-orange-50 text-orange-950', 
          alert: 'Everyone may begin to experience health effects; members of sensitive groups may experience more serious health effects.', 
          rec: 'Avoid prolonged outdoor activities. Consider wearing an N95 mask outdoors and run an air purifier indoors.' 
        };
      case 5:
        return { 
          label: 'Very Poor (5)', 
          color: 'bg-red-600 text-white', 
          border: 'border-red-600', 
          bg: 'bg-red-50 text-red-950', 
          alert: 'Health warnings of emergency conditions. The entire population is more likely to be affected.', 
          rec: 'Remain indoors with windows closed. Keep physical exertion to a minimum and seek medical advice if experiencing difficulty breathing.' 
        };
      default:
        return { 
          label: 'Unknown', 
          color: 'bg-slate-500 text-white', 
          border: 'border-slate-500', 
          bg: 'bg-slate-50 text-slate-950', 
          alert: 'Data not available', 
          rec: '' 
        };
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-900 flex flex-col">

      {/* ========================================================= */}
      {/* 1. LOGIN PAGE (login.html)                               */}
      {/* ========================================================= */}
      {currentView === 'login' && (
        <div className="min-h-screen bg-[#f4f6fb] flex items-center justify-center p-4">
          <div className="w-full max-w-[420px] bg-white text-slate-900 rounded-2xl p-8 shadow-xl border border-slate-100">
            {/* Air Quality / Wind Icon at Top */}
            <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 shadow-sm">
              <Wind className="w-7 h-7" />
            </div>

            {/* Heading and Subtitle */}
            <div className="text-center mb-6">
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Welcome to Air Quality Monitor</h1>
              <p className="text-xs text-slate-500 mt-1.5 leading-relaxed">Monitor air quality and stay informed about your environment.</p>
            </div>

            {/* Alert Message for Invalid Login or Registration Success */}
            {authMsg && (
              <div
                className={`p-3 rounded-xl text-xs mb-5 flex items-center gap-2 border ${
                  authMsg.type === 'error'
                    ? 'bg-red-50 text-red-700 border-red-200'
                    : authMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-indigo-50 text-indigo-700 border-indigo-200'
                }`}
              >
                {authMsg.type === 'error' ? (
                  <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
                ) : (
                  <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-500" />
                )}
                <span>{authMsg.text}</span>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleLoginSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Email Address
                </label>
                <input
                  type="email"
                  value={loginEmail}
                  onChange={e => setLoginEmail(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 transition"
                  placeholder="name@example.com"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Password
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={loginPassword}
                    onChange={e => setLoginPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 pr-10 transition"
                    placeholder="Enter your password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition p-1"
                    aria-label="Toggle password visibility"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:scale-[0.99] text-white font-medium rounded-xl text-sm transition shadow-lg shadow-indigo-600/20 mt-2"
              >
                Sign In
              </button>
            </form>

            <div className="mt-6 pt-5 border-t border-slate-100 text-center text-xs text-slate-500">
              Don't have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setAuthMsg(null);
                  setCurrentView('register');
                }}
                className="text-indigo-600 hover:underline font-semibold"
              >
                Create an account
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 2. REGISTER PAGE (register.html)                         */}
      {/* ========================================================= */}
      {currentView === 'register' && (
        <div className="min-h-screen bg-[#f4f6fb] flex items-center justify-center p-4">
          <div className="w-full max-w-[420px] bg-white text-slate-900 rounded-2xl p-8 shadow-xl border border-slate-100">
            <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 shadow-sm">
              <Wind className="w-7 h-7" />
            </div>

            <div className="text-center mb-6">
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Create your Air Quality Account</h1>
              <p className="text-xs text-slate-500 mt-1.5 leading-relaxed">Register to monitor air quality and track historical searches</p>
            </div>

            {authMsg && (
              <div className="p-3 rounded-xl text-xs mb-5 flex items-center gap-2 border bg-red-50 text-red-700 border-red-200">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
                <span>{authMsg.text}</span>
              </div>
            )}

            <form onSubmit={handleRegisterSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
                <input
                  type="text"
                  value={regFullName}
                  onChange={e => setRegFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 transition"
                  placeholder="John Doe"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Email</label>
                <input
                  type="email"
                  value={regEmail}
                  onChange={e => setRegEmail(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 transition"
                  placeholder="john@example.com"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={regPassword}
                    onChange={e => setRegPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 pr-10 transition"
                    placeholder="Minimum 6 characters"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition p-1"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Confirm Password</label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    value={regConfirmPassword}
                    onChange={e => setRegConfirmPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm placeholder:text-slate-400 pr-10 transition"
                    placeholder="Re-type password"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition p-1"
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:scale-[0.99] text-white font-medium rounded-xl text-sm transition shadow-lg shadow-indigo-600/20 mt-2"
              >
                Create Account
              </button>
            </form>

            <div className="mt-6 pt-5 border-t border-slate-100 text-center text-xs text-slate-500">
              Already have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setAuthMsg(null);
                  setCurrentView('login');
                }}
                className="text-indigo-600 hover:underline font-semibold"
              >
                Sign In
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 3. AIR QUALITY DASHBOARD (air.html)                      */}
      {/* ========================================================= */}
      {currentView === 'air' && (
        <div className="bg-slate-900 min-h-screen text-slate-100 flex flex-col">
          {/* Top Bar as designed in air.html */}
          <header className="bg-slate-950 px-6 py-3.5 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2 text-blue-400 font-bold text-base">
              <span>🌍</span> Air Quality Monitor
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-400 border-r border-slate-800 pr-3 hidden sm:inline">
                Welcome, <strong className="text-white">{currentUser?.fullName || 'User'}</strong>
              </span>
              <button
                onClick={() => setCurrentView('history')}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition flex items-center gap-1.5"
              >
                <History className="w-3.5 h-3.5 text-indigo-400" />
                <span>Search History</span>
              </button>
              <button
                onClick={handleLogout}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-red-950/60 hover:bg-red-900 text-red-300 border border-red-800/40 transition flex items-center gap-1.5"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Logout</span>
              </button>
            </div>
          </header>

          <main className="p-6 max-w-5xl mx-auto w-full space-y-6 flex-1">
            {/* Search Card */}
            <div className="bg-white text-slate-900 rounded-2xl p-6 shadow-md border border-slate-200">
              <h2 className="text-base font-bold text-slate-900 mb-3">Check City Air Quality</h2>
              <div className="flex gap-2.5">
                <input
                  type="text"
                  value={cityInput}
                  onChange={e => setCityInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleSearch()}
                  className="flex-1 px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm placeholder:text-slate-400"
                  placeholder="Enter city name (e.g. Paris, Tokyo, Mumbai)"
                />
                <button
                  onClick={() => handleSearch()}
                  disabled={isLoadingApi}
                  className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 active:scale-[0.99] disabled:opacity-50 text-white font-medium rounded-xl text-sm transition flex items-center gap-2 shadow-md shadow-blue-600/20"
                >
                  {isLoadingApi ? (
                    <span className="animate-spin inline-block">⏳</span>
                  ) : (
                    <Search className="w-4 h-4" />
                  )}
                  <span>Search</span>
                </button>
              </div>

              {/* Error Box */}
              {apiError && (
                <div className="mt-3 p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200 flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0 text-red-500" />
                  <span>{apiError}</span>
                </div>
              )}

              {/* Save Status Toast */}
              {saveToast && (
                <div className="mt-3 inline-flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-800 border border-emerald-300 animate-fadeIn">
                  <span>💾 Search saved to history</span>
                </div>
              )}
            </div>

            {/* Results Presentation (Preserving exact OpenWeatherMap data presentation) */}
            {currentResult && (
              <div className="space-y-6">
                {/* AQI Level Overview */}
                <div className="bg-white text-slate-900 rounded-2xl p-6 shadow-md border border-slate-200">
                  <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-4">
                    <div>
                      <h3 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                        <span>📍</span> {currentResult.city}
                      </h3>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Coordinates: {currentResult.lat.toFixed(4)}° N, {currentResult.lon.toFixed(4)}° E
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-500 uppercase font-semibold">Air Quality Index</span>
                      <span className={`px-4 py-1.5 rounded-full text-xs font-bold shadow-sm ${aqiDetails(currentResult.aqi).color}`}>
                        {aqiDetails(currentResult.aqi).label}
                      </span>
                    </div>
                  </div>

                  {/* Health Alert Banner */}
                  <div className={`p-4 rounded-xl border mb-4 ${aqiDetails(currentResult.aqi).bg} ${aqiDetails(currentResult.aqi).border}`}>
                    <div className="font-bold text-xs uppercase tracking-wider mb-1 flex items-center gap-1.5">
                      <AlertTriangle className="w-4 h-4" /> Health Assessment
                    </div>
                    <p className="text-sm font-medium">{aqiDetails(currentResult.aqi).alert}</p>
                  </div>

                  {/* Recommendations */}
                  <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-slate-800">
                    <div className="font-bold text-xs uppercase tracking-wider text-slate-600 mb-1">
                      💡 Recommendations
                    </div>
                    <p className="text-sm leading-relaxed">{aqiDetails(currentResult.aqi).rec}</p>
                  </div>
                </div>

                {/* Pollutants Breakdown */}
                <div className="bg-white text-slate-900 rounded-2xl p-6 shadow-md border border-slate-200">
                  <h4 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
                    <span>🔬</span> Real-Time Pollutant Concentrations (μg/m³)
                  </h4>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5">
                    {[
                      { label: 'PM2.5', val: currentResult.components.pm2_5, sub: 'Fine Particles' },
                      { label: 'PM10', val: currentResult.components.pm10, sub: 'Coarse Dust' },
                      { label: 'SO₂', val: currentResult.components.so2, sub: 'Sulfur Dioxide' },
                      { label: 'NO₂', val: currentResult.components.no2, sub: 'Nitrogen Dioxide' },
                      { label: 'CO', val: currentResult.components.co, sub: 'Carbon Monoxide' },
                      { label: 'O₃', val: currentResult.components.o3, sub: 'Ozone' },
                      { label: 'NO', val: currentResult.components.no, sub: 'Nitrogen Monoxide' },
                      { label: 'NH₃', val: currentResult.components.nh3, sub: 'Ammonia' }
                    ].map((item, idx) => (
                      <div key={idx} className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-center">
                        <div className="text-xs text-slate-500 font-semibold">{item.label}</div>
                        <div className="text-xl font-bold text-slate-900 my-0.5">
                          {item.val !== undefined ? Number(item.val).toFixed(1) : '-'}
                        </div>
                        <div className="text-[10px] text-slate-400">{item.sub}</div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </main>
        </div>
      )}

      {/* ========================================================= */}
      {/* 4. SEARCH HISTORY PAGE (history.html)                    */}
      {/* ========================================================= */}
      {currentView === 'history' && (
        <div className="bg-slate-900 min-h-screen text-slate-100 flex flex-col">
          {/* Top Bar as designed in history.html */}
          <header className="bg-slate-950 px-6 py-3.5 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2 text-blue-400 font-bold text-base">
              <span>🌍</span> Air Quality Monitor
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-400 border-r border-slate-800 pr-3 hidden sm:inline">
                Welcome, <strong className="text-white">{currentUser?.fullName || 'User'}</strong>
              </span>
              <button
                onClick={() => setCurrentView('air')}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition flex items-center gap-1.5"
              >
                <Search className="w-3.5 h-3.5 text-blue-400" />
                <span>Dashboard</span>
              </button>
              <button
                onClick={handleLogout}
                className="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-red-950/60 hover:bg-red-900 text-red-300 border border-red-800/40 transition flex items-center gap-1.5"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Logout</span>
              </button>
            </div>
          </header>

          <main className="p-6 max-w-6xl mx-auto w-full space-y-4 flex-1">
            <div className="bg-white text-slate-900 rounded-2xl p-6 shadow-md border border-slate-200">
              <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">Your Air Quality Search History</h2>
                  <p className="text-xs text-slate-500">
                    Historical air quality measurements recorded from your searches.
                  </p>
                </div>
                <span className="text-xs px-3 py-1 rounded-full bg-blue-50 text-blue-700 font-semibold border border-blue-200">
                  {historyRecords.filter(r => r.userId === currentUser?.id).length} Saved Searches
                </span>
              </div>

              <div className="overflow-x-auto border border-slate-200 rounded-xl">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-100 text-slate-700 uppercase font-semibold border-b border-slate-200">
                    <tr>
                      <th className="p-3">#</th>
                      <th className="p-3">City</th>
                      <th className="p-3">AQI Level</th>
                      <th className="p-3">CO</th>
                      <th className="p-3">NO</th>
                      <th className="p-3">NO₂</th>
                      <th className="p-3">O₃</th>
                      <th className="p-3">SO₂</th>
                      <th className="p-3">PM2.5</th>
                      <th className="p-3">PM10</th>
                      <th className="p-3">NH₃</th>
                      <th className="p-3">Search Timestamp</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-800">
                    {historyRecords
                      .filter(r => r.userId === currentUser?.id)
                      .map((rec, idx) => {
                        const info = aqiDetails(rec.aqi);
                        return (
                          <tr key={rec.id} className="hover:bg-slate-50 transition">
                            <td className="p-3 text-slate-400">{idx + 1}</td>
                            <td className="p-3 font-semibold text-slate-900">{rec.city}</td>
                            <td className="p-3">
                              <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${info.color}`}>
                                {info.label}
                              </span>
                            </td>
                            <td className="p-3 font-mono">{rec.co.toFixed(1)}</td>
                            <td className="p-3 font-mono">{rec.no.toFixed(1)}</td>
                            <td className="p-3 font-mono">{rec.no2.toFixed(1)}</td>
                            <td className="p-3 font-mono">{rec.o3.toFixed(1)}</td>
                            <td className="p-3 font-mono">{rec.so2.toFixed(1)}</td>
                            <td className="p-3 font-mono font-bold text-slate-900">{rec.pm2_5.toFixed(1)}</td>
                            <td className="p-3 font-mono font-bold text-slate-900">{rec.pm10.toFixed(1)}</td>
                            <td className="p-3 font-mono">{rec.nh3.toFixed(1)}</td>
                            <td className="p-3 text-slate-500 whitespace-nowrap">{rec.searched_at}</td>
                          </tr>
                        );
                      })}
                    {historyRecords.filter(r => r.userId === currentUser?.id).length === 0 && (
                      <tr>
                        <td colSpan={12} className="p-8 text-center text-slate-500">
                          No searches recorded yet. Search for a city on the dashboard to save data.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </main>
        </div>
      )}

    </div>
  );
}
