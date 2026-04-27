const injectStyles = () => {
  if (document.getElementById('hospital-styles')) return;

  const css = `
    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=DM+Sans:wght@300;400;500;600&display=swap');

    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    :root {
      --bg: #f0ebe3;
      --bg-card: #faf8f5;
      --bg-dark: #1a1410;
      --primary: #c5522a;
      --primary-light: #e8744e;
      --accent: #3d7a5e;
      --accent-light: #5aaa84;
      --gold: #d4a84b;
      --text: #2d2520;
      --text-muted: #7a6e68;
      --border: #ddd4c8;
      --shadow: 0 4px 24px rgba(45,37,32,0.08);
      --shadow-lg: 0 12px 48px rgba(45,37,32,0.14);
      --radius: 12px;
      --radius-lg: 20px;
    }

    body {
      font-family: 'DM Sans', sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      font-size: 15px;
      line-height: 1.6;
    }

    /* Layout */
    .layout { display: flex; min-height: 100vh; }
    .sidebar {
      width: 260px; min-height: 100vh;
      background: var(--bg-dark);
      color: #f0ebe3;
      display: flex; flex-direction: column;
      position: fixed; left: 0; top: 0; bottom: 0;
      z-index: 100;
      transition: transform 0.3s ease;
    }
    .main-content {
      margin-left: 260px;
      flex: 1;
      padding: 32px;
      max-width: calc(100vw - 260px);
    }

    /* Sidebar */
    .sidebar-header { padding: 28px 24px; border-bottom: 1px solid rgba(255,255,255,0.08); }
    .sidebar-logo { font-family: 'Playfair Display', serif; font-size: 20px; font-weight: 700; color: #f0ebe3; line-height: 1.2; }
    .sidebar-tagline { font-size: 11px; color: rgba(240,235,227,0.45); text-transform: uppercase; letter-spacing: 1.5px; margin-top: 4px; }
    .sidebar-nav { flex: 1; padding: 16px 0; }
    .nav-section-label { font-size: 10px; text-transform: uppercase; letter-spacing: 2px; color: rgba(240,235,227,0.3); padding: 12px 24px 6px; font-weight: 600; }
    .nav-item {
      display: flex; align-items: center; gap: 12px;
      padding: 11px 24px; color: rgba(240,235,227,0.65);
      cursor: pointer; border: none; background: none; width: 100%;
      text-align: left; font-family: 'DM Sans', sans-serif;
      font-size: 14px; font-weight: 500; transition: all 0.2s;
      border-left: 3px solid transparent;
    }
    .nav-item:hover { color: #f0ebe3; background: rgba(255,255,255,0.05); }
    .nav-item.active { color: #f0ebe3; background: rgba(197,82,42,0.15); border-left-color: var(--primary); }
    .nav-icon { width: 18px; height: 18px; flex-shrink: 0; }

    /* Page Header */
    .page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 28px; }
    .page-title { font-family: 'Playfair Display', serif; font-size: 28px; font-weight: 700; color: var(--text); line-height: 1.2; }
    .page-subtitle { color: var(--text-muted); font-size: 14px; margin-top: 4px; }

    /* Cards */
    .card { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow); padding: 24px; border: 1px solid var(--border); }
    .card-title { font-family: 'Playfair Display', serif; font-size: 17px; font-weight: 600; margin-bottom: 16px; }

    /* Stats Grid */
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 28px; }
    .stat-card { background: var(--bg-card); border-radius: var(--radius); padding: 20px; border: 1px solid var(--border); box-shadow: var(--shadow); position: relative; overflow: hidden; }
    .stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; }
    .stat-card.primary::before { background: var(--primary); }
    .stat-card.accent::before { background: var(--accent); }
    .stat-card.gold::before { background: var(--gold); }
    .stat-card.dark::before { background: var(--bg-dark); }
    .stat-value { font-family: 'Playfair Display', serif; font-size: 32px; font-weight: 700; color: var(--text); line-height: 1; }
    .stat-label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.8px; margin-top: 6px; font-weight: 500; }
    .stat-icon { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); font-size: 36px; opacity: 0.07; }

    /* Grid */
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }

    /* Table */
    .table-wrap { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow); border: 1px solid var(--border); overflow: hidden; }
    .table-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border); }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; padding: 12px 20px; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: var(--text-muted); font-weight: 600; background: rgba(0,0,0,0.015); border-bottom: 1px solid var(--border); }
    td { padding: 14px 20px; border-bottom: 1px solid rgba(0,0,0,0.04); font-size: 14px; vertical-align: middle; }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: rgba(0,0,0,0.015); }

    /* Badges */
    .badge { display: inline-flex; align-items: center; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .badge-pending { background: #fff8e6; color: #c07b00; }
    .badge-confirmed { background: #e6f4ee; color: #1a7a4a; }
    .badge-completed { background: #e6ecf4; color: #1a4a7a; }
    .badge-cancelled { background: #fcecea; color: #c0220a; }
    .badge-open { background: #fff8e6; color: #c07b00; }
    .badge-resolved { background: #e6f4ee; color: #1a7a4a; }
    .badge-in_progress { background: #efe6ff; color: #5a0ac0; }
    .badge-general { background: #f0f0f0; color: #555; }
    .badge-urgent { background: #fcecea; color: #c0220a; }
    .badge-high { background: #fff0e6; color: #c05000; }
    .badge-normal { background: #f0f0f0; color: #555; }
    .badge-low { background: #e6f4ee; color: #1a7a4a; }

    /* Buttons */
    .btn { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; border-radius: var(--radius); font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 600; cursor: pointer; border: none; transition: all 0.2s; text-decoration: none; }
    .btn-primary { background: var(--primary); color: white; }
    .btn-primary:hover { background: var(--primary-light); transform: translateY(-1px); box-shadow: 0 4px 16px rgba(197,82,42,0.35); }
    .btn-secondary { background: transparent; color: var(--text); border: 1.5px solid var(--border); }
    .btn-secondary:hover { background: var(--border); }
    .btn-accent { background: var(--accent); color: white; }
    .btn-accent:hover { background: var(--accent-light); }
    .btn-sm { padding: 6px 14px; font-size: 12px; }
    .btn-danger { background: #c0220a; color: white; }

    /* Forms */
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .form-group { display: flex; flex-direction: column; gap: 6px; }
    .form-group.full { grid-column: 1 / -1; }
    label { font-size: 13px; font-weight: 600; color: var(--text); }
    input, select, textarea { padding: 10px 14px; border: 1.5px solid var(--border); border-radius: var(--radius); font-family: 'DM Sans', sans-serif; font-size: 14px; color: var(--text); background: white; transition: border-color 0.2s; outline: none; }
    input:focus, select:focus, textarea:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(197,82,42,0.1); }
    textarea { resize: vertical; min-height: 80px; }

    /* Doctor Cards */
    .doctor-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
    .doctor-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: 24px; border: 1px solid var(--border); box-shadow: var(--shadow); transition: all 0.25s; }
    .doctor-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); border-color: var(--primary); }
    .doctor-avatar { width: 56px; height: 56px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--gold)); display: flex; align-items: center; justify-content: center; font-family: 'Playfair Display', serif; font-size: 20px; font-weight: 700; color: white; margin-bottom: 12px; }
    .doctor-name { font-family: 'Playfair Display', serif; font-size: 16px; font-weight: 600; margin-bottom: 2px; }
    .doctor-spec { font-size: 13px; color: var(--text-muted); }
    .doctor-dept { font-size: 12px; color: var(--accent); font-weight: 600; margin-top: 8px; }
    .doctor-fee { font-size: 15px; font-weight: 700; color: var(--primary); margin-top: 12px; }

    /* Time Slots */
    .slots-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 8px; margin-top: 12px; }
    .slot-btn { padding: 8px; text-align: center; border-radius: 8px; border: 1.5px solid var(--border); background: white; font-size: 13px; cursor: pointer; transition: all 0.15s; font-weight: 500; }
    .slot-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
    .slot-btn.selected { background: var(--primary); border-color: var(--primary); color: white; }
    .slot-btn:disabled { background: #f5f5f5; color: #bbb; cursor: not-allowed; text-decoration: line-through; }

    /* Modal */
    .modal-overlay { position: fixed; inset: 0; background: rgba(26,20,16,0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
    .modal { background: var(--bg-card); border-radius: var(--radius-lg); padding: 32px; width: 100%; max-width: 600px; max-height: 90vh; overflow-y: auto; box-shadow: var(--shadow-lg); }
    .modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    .modal-title { font-family: 'Playfair Display', serif; font-size: 22px; font-weight: 700; }
    .modal-close { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-muted); padding: 4px; line-height: 1; }

    /* Alerts */
    .alert { padding: 12px 16px; border-radius: var(--radius); font-size: 14px; margin-bottom: 16px; }
    .alert-success { background: #e6f4ee; color: #1a5c37; border-left: 4px solid var(--accent); }
    .alert-error { background: #fcecea; color: #8b1a0d; border-left: 4px solid #c0220a; }
    .alert-info { background: #e6ecf4; color: #1a3c6a; border-left: 4px solid #3d6fa8; }

    /* Search */
    .search-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
    .search-input-wrap { position: relative; flex: 1; }
    .search-input-wrap input { width: 100%; padding-left: 40px; }
    .search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }

    /* Department select cards */
    .dept-select-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; margin-top: 8px; }
    .dept-card { padding: 14px 12px; border-radius: 10px; border: 2px solid var(--border); background: white; cursor: pointer; text-align: center; transition: all 0.2s; }
    .dept-card:hover { border-color: var(--primary); }
    .dept-card.selected { border-color: var(--primary); background: rgba(197,82,42,0.06); }
    .dept-card-name { font-weight: 600; font-size: 13px; }
    .dept-card-floor { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

    /* Step Indicator */
    .steps { display: flex; align-items: center; gap: 0; margin-bottom: 28px; background: var(--bg-card); border-radius: var(--radius-lg); padding: 16px 24px; border: 1px solid var(--border); }
    .step { display: flex; align-items: center; gap: 10px; flex: 1; }
    .step:not(:last-child)::after { content: '→'; margin: 0 8px; color: var(--border); font-size: 18px; }
    .step-num { width: 28px; height: 28px; border-radius: 50%; background: var(--border); color: var(--text-muted); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0; }
    .step.active .step-num { background: var(--primary); color: white; }
    .step.done .step-num { background: var(--accent); color: white; }
    .step-label { font-size: 13px; font-weight: 600; color: var(--text-muted); }
    .step.active .step-label { color: var(--text); }

    /* Empty state */
    .empty-state { text-align: center; padding: 60px 20px; color: var(--text-muted); }
    .empty-state-icon { font-size: 48px; margin-bottom: 12px; }
    .empty-state-title { font-family: 'Playfair Display', serif; font-size: 18px; margin-bottom: 6px; }

    /* Loading */
    .loading { display: flex; align-items: center; justify-content: center; padding: 48px; color: var(--text-muted); gap: 12px; }
    .spinner { width: 22px; height: 22px; border: 2.5px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Tabs */
    .tabs { display: flex; gap: 4px; margin-bottom: 20px; }
    .tab { padding: 9px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; border: none; background: none; color: var(--text-muted); font-family: 'DM Sans', sans-serif; transition: all 0.15s; }
    .tab:hover { background: var(--border); color: var(--text); }
    .tab.active { background: var(--primary); color: white; }

    .token-badge { font-family: 'DM Sans', sans-serif; font-size: 12px; font-weight: 700; background: var(--bg-dark); color: var(--gold); padding: 3px 10px; border-radius: 6px; letter-spacing: 1px; }

    @media (max-width: 768px) {
      .sidebar { transform: translateX(-100%); }
      .main-content { margin-left: 0; max-width: 100vw; padding: 16px; }
      .form-grid { grid-template-columns: 1fr; }
      .grid-2, .grid-3 { grid-template-columns: 1fr; }
      .stats-grid { grid-template-columns: repeat(2, 1fr); }

      /* Step Indicator — Mobile */
      .steps { overflow-x: auto; overflow-y: hidden; scrollbar-width: none; padding: 12px 16px; gap: 4px; }
      .steps::-webkit-scrollbar { display: none; }
      .step { flex: 0 0 auto; min-width: fit-content; gap: 6px; }
      .step:not(:last-child)::after { margin: 0 6px; font-size: 14px; }
      .step-num { width: 24px; height: 24px; font-size: 11px; }
      .step-label { font-size: 11px; }
    }
  `;

  const style = document.createElement('style');
  style.id = 'hospital-styles';
  style.textContent = css;
  document.head.appendChild(style);
};

export default injectStyles;
