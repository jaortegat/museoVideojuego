(function(){
    function ordinalWithSuffix(n) {
        if (!n || n < 1) return '';
        var v = n % 100;
        var suffix = 'TH';
        if (v < 11 || v > 13) {
            var rem = n % 10;
            if (rem === 1) suffix = 'ST';
            else if (rem === 2) suffix = 'ND';
            else if (rem === 3) suffix = 'RD';
        }
        return String(n) + suffix;
    }

    function truncateName(s, max) {
        if (!s) return '';
        var str = s.toString();
        if (str.length <= max) return str.toUpperCase();
        return str.substring(0, max).toUpperCase();
    }

    var names = Array.isArray(window.consoleNames) ? window.consoleNames : [];
    var counts = Array.isArray(window.consoleCounts) ? window.consoleCounts : [];
    var ctx = document.getElementById('consolePie');
    if (!ctx) return;
    var bg = [ '#4e73df','#1cc88a','#36b9cc','#f6c23e','#e74a3b','#858796' ];

    function createChart(initialNames, initialCounts) {
        var availableHeight = ctx.clientHeight || Math.floor(window.innerHeight * 0.7) || 400;
        var baseThickness = Math.max(24, Math.floor((availableHeight / Math.max(1, initialNames.length)) * 0.7));
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels: initialNames,
                datasets: [{
                    label: 'Votos',
                    data: initialCounts,
                    backgroundColor: initialNames.map(function(_,i){ return bg[i % bg.length]; }),
                    borderWidth: 0,
                    barThickness: baseThickness
                }]
            },
            options: {
                responsive: true,
                indexAxis: 'y',
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: false },
                    datalabels: {
                        anchor: 'end',
                        align: 'right',
                        formatter: function(value) { return value; },
                        font: { weight: 'bold', size: 12 }
                    }
                },
                scales: {
                    x: { title: { display: true, text: 'Votos' }, beginAtZero: true, precision: 0 },
                    y: { title: { display: true, text: 'Consolas' } }
                }
            },
            plugins: [ChartDataLabels]
        });
    }

    var chart = createChart(names, counts);

    function recalcBarThickness() {
        var h = ctx.clientHeight || Math.floor(window.innerHeight * 0.7) || 400;
        var newThickness = Math.max(24, Math.floor((h / Math.max(1, chart.data.labels.length)) * 0.7));
        chart.data.datasets.forEach(function(ds){ ds.barThickness = newThickness; });
        chart.update();
    }

    // Recalculate on resize so bars stay proportionate
    var resizeTimer = null;
    window.addEventListener('resize', function(){
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(recalcBarThickness, 150);
    });

    // preload latest results from server API to ensure chart starts up-to-date
    fetch('/api/results').then(function(resp){ return resp.json(); }).then(function(json){
        if (json && Array.isArray(json.results)) {
            var res = json.results.filter(function(r){ return r.votes && r.votes > 0; });
            var n = res.map(function(r){ return r.console; });
            var c = res.map(function(r){ return r.votes; });
            chart.data.labels = n;
            chart.data.datasets[0].data = c;
            chart.data.datasets[0].backgroundColor = n.map(function(_,i){ return bg[i % bg.length]; });
            recalcBarThickness();
            chart.update();
        }
    }).catch(function(e){ console.warn('Failed to preload results', e); });

    // load leaderboards for each game from server so DB contents appear immediately
    document.querySelectorAll('.col-12.col-sm-6.col-md-4').forEach(function(col){
        var game = col.getAttribute('data-game');
        if (!game) return;
        fetch('/api/leaderboard?game=' + encodeURIComponent(game)).then(function(r){ return r.json(); }).then(function(json){
            if (!json || !Array.isArray(json.top)) return;
            var tbody = col.querySelector('tbody');
            if (!tbody) return;
            // clear existing
            while (tbody.firstChild) tbody.removeChild(tbody.firstChild);
            if (json.top.length === 0) {
                var er = document.createElement('tr');
                var td = document.createElement('td'); td.setAttribute('colspan','3'); td.textContent = 'Sin puntuaciones';
                er.appendChild(td);
                tbody.appendChild(er);
            } else {
                json.top.forEach(function(item, idx){
                    var tr = document.createElement('tr');
                    var ord = document.createElement('td'); ord.className = 'ordinal'; ord.textContent = ordinalWithSuffix(idx + 1);
                    var s = document.createElement('td'); s.className = 'score'; s.textContent = item.score;
                    var p = document.createElement('td'); p.className = 'player'; p.textContent = truncateName(item.player, 8);
                    tr.appendChild(ord); tr.appendChild(s); tr.appendChild(p);
                    tbody.appendChild(tr);
                });
            }
        }).catch(function(e){ console.warn('Failed to load leaderboard for', game, e); });
    });

    // Post-process server-rendered rows (Thymeleaf) so ordinals have suffixes and names are truncated
    document.querySelectorAll('.leaderboard-table').forEach(function(table){
        var rows = table.querySelectorAll('tbody tr');
        rows.forEach(function(tr, idx){
            // if row already has 3 cells (ordinal, score, player) skip; otherwise adapt
            var cells = tr.querySelectorAll('td');
            if (cells.length === 3) {
                var ordCell = cells[0];
                // if ordinal cell contains plain number, replace with suffix version
                var num = parseInt(ordCell.textContent || '');
                if (!isNaN(num)) ordCell.textContent = ordinalWithSuffix(num);
                var playerCell = cells[2];
                if (playerCell) playerCell.textContent = truncateName(playerCell.textContent, 8);
            } else if (cells.length === 2) {
                // legacy server-rendered two-column layout: convert to ordinal/score/player
                var nameCell = cells[0];
                var scoreCell = cells[1];
                // replace row content
                while (tr.firstChild) tr.removeChild(tr.firstChild);
                var ord = document.createElement('td'); ord.className = 'ordinal'; ord.textContent = ordinalWithSuffix(idx + 1);
                var s = document.createElement('td'); s.className = 'score'; s.textContent = scoreCell.textContent;
                var p = document.createElement('td'); p.className = 'player'; p.textContent = truncateName(nameCell.textContent, 8);
                tr.appendChild(ord); tr.appendChild(s); tr.appendChild(p);
            }
        });
    });

    // SSE updates: update chart when vote arrives
    if (!!window.EventSource) {
        var es = new EventSource('/results-stream');
        es.addEventListener('vote', function(evt){
            try {
                var data = JSON.parse(evt.data);
                var name = data.console;
                var votes = data.votes;
                // remove consoles with zero votes
                if (!votes || votes <= 0) {
                    var remIdx = chart.data.labels.indexOf(name);
                    if (remIdx !== -1) {
                        chart.data.labels.splice(remIdx, 1);
                        chart.data.datasets[0].data.splice(remIdx, 1);
                        chart.data.datasets[0].backgroundColor.splice(remIdx, 1);
                        recalcBarThickness();
                        chart.update();
                    }
                    return;
                }
                var idx = chart.data.labels.indexOf(name);
                if (idx === -1) {
                    chart.data.labels.push(name);
                    chart.data.datasets[0].data.push(votes);
                    chart.data.datasets[0].backgroundColor.push(bg[(chart.data.labels.length-1) % bg.length]);
                } else {
                    chart.data.datasets[0].data[idx] = votes;
                }
                recalcBarThickness();
                chart.update();
            } catch(e) { console.warn('Invalid SSE payload', evt.data); }
        });
        // listen for live score events and update leaderboards
        es.addEventListener('score', function(evt){
            try {
                var data = JSON.parse(evt.data);
                var game = (data.videogame || '').toString().trim().toLowerCase();
                var player = data.player;
                var score = data.score;
                // find the leaderboard column whose heading text matches the videogame
                var cols = document.querySelectorAll('.col-12.col-sm-6.col-md-4');
                for (var i = 0; i < cols.length; i++) {
                    var col = cols[i];
                    var dataGame = col.getAttribute('data-game') || '';
                    dataGame = dataGame.toString().trim().toLowerCase();
                    if (dataGame === game) {
                        // fetch updated top-10 for this game from server and replace tbody
                        fetch('/api/leaderboard?game=' + encodeURIComponent(data.videogame)).then(function(r){ return r.json(); }).then(function(json){
                            if (!json || !Array.isArray(json.top)) return;
                            var tbody = col.querySelector('tbody');
                            if (!tbody) return;
                            // clear existing rows
                            while (tbody.firstChild) tbody.removeChild(tbody.firstChild);
                            if (json.top.length === 0) {
                                var er = document.createElement('tr');
                                var td = document.createElement('td'); td.setAttribute('colspan','3'); td.textContent = 'Sin puntuaciones';
                                er.appendChild(td);
                                tbody.appendChild(er);
                            } else {
                                json.top.forEach(function(item, idx){
                                    var tr = document.createElement('tr');
                                    var ord = document.createElement('td'); ord.className = 'ordinal'; ord.textContent = ordinalWithSuffix(idx + 1);
                                    var s = document.createElement('td'); s.className = 'score'; s.textContent = item.score;
                                    var p = document.createElement('td'); p.className = 'player'; p.textContent = truncateName(item.player, 8);
                                    tr.appendChild(ord); tr.appendChild(s); tr.appendChild(p);
                                    tbody.appendChild(tr);
                                });
                            }
                        }).catch(function(e){ console.warn('Failed to fetch leaderboard', e); });
                        break;
                    }
                }
            } catch (e) { console.warn('Invalid score SSE payload', evt.data); }
        });
        es.onerror = function(e){ console.warn('SSE error', e); };
    }
})();
