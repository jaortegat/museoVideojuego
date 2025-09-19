(function () {
    // MAX_VOTES will be provided by the server as a global variable in the template
    var MAX_VOTES = window.MAX_VOTES || 3;
    var selectedConsoles = new Set();

    function updateInstruction() {
        var instr = document.getElementById('maxVotesInstruction');
        if (instr) instr.textContent = 'Selecciona hasta ' + MAX_VOTES + ' consola(s)';
    }

    function toggleSelection(card) {
        var consoleName = card.getAttribute('data-value');
        var isPressed = card.getAttribute('aria-pressed') === 'true';
        if (isPressed) {
            selectedConsoles.delete(consoleName);
            card.classList.remove('selected');
            card.setAttribute('aria-pressed', 'false');
        } else if (selectedConsoles.size < MAX_VOTES) {
            selectedConsoles.add(consoleName);
            card.classList.add('selected');
            card.setAttribute('aria-pressed', 'true');
        } else {
            alert('Solo puedes seleccionar hasta ' + MAX_VOTES + ' consola(s)');
        }
        updateSelectionCount();
    }

    function updateSelectionCount() {
        var cnt = document.getElementById('selectionCount');
        if (cnt) cnt.textContent = selectedConsoles.size + '/' + MAX_VOTES;
    }

    function validateAndSubmit() {
        if (selectedConsoles.size === 0) {
            alert('Por favor, selecciona al menos 1 consola antes de votar.');
            return;
        }
        if (selectedConsoles.size > MAX_VOTES) {
            alert('Solo puedes seleccionar hasta ' + MAX_VOTES + ' consola(s)');
            return;
        }
        var payload = { selectedConsoles: Array.from(selectedConsoles) };
        fetch('/api/vote', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(function(resp){
            if (resp.ok) {
                // clear selections and update UI
                selectedConsoles.clear();
                document.querySelectorAll('.card-select.selected').forEach(function(el){
                    el.classList.remove('selected');
                    el.setAttribute('aria-pressed','false');
                });
                updateSelectionCount();
                // brief visual confirmation
                var btn = document.querySelector('#voteForm .btn');
                if (btn) {
                    var old = btn.textContent;
                    btn.textContent = 'Enviado';
                    setTimeout(function(){ btn.textContent = old; }, 1400);
                }
            } else {
                alert('Error al enviar el voto');
            }
        }).catch(function(){
            alert('No se pudo conectar con el servidor');
        });
    }

    function submitScores(e) {
        if (e && e.preventDefault) e.preventDefault();
        var form = document.getElementById('sideForm');
        if (!form) return;
        // Build JSON payload: playerName and scores[] by reading inputs named scores[...] in order
        var payload = { playerName: '', scores: [] };
        var pn = form.querySelector('#playerName');
        if (pn) payload.playerName = pn.value ? pn.value.trim() : '';

        // validate player name is required
        if (!payload.playerName) {
            if (pn) {
                pn.classList.add('is-invalid');
                var nf = pn.parentNode.querySelector('.invalid-feedback');
                if (nf) nf.style.display = 'block';
                pn.focus();
            }
            if (sideFeedback) { sideFeedback.style.display = 'block'; sideFeedback.textContent = 'El nombre del jugador es obligatorio.'; }
            return;
        } else {
            if (pn) {
                pn.classList.remove('is-invalid');
                var nf = pn.parentNode.querySelector('.invalid-feedback');
                if (nf) nf.style.display = 'none';
            }
        }
        // gather score inputs in DOM order and validate: only integers 0..9,999,999 allowed
        var invalidFound = false;
        var firstInvalid = null;
        // clear global feedback
        var sideFeedback = document.getElementById('sideFormFeedback');
        if (sideFeedback) { sideFeedback.style.display = 'none'; sideFeedback.textContent = ''; }

        form.querySelectorAll('input[name^="scores"]').forEach(function(inp){
            // clear previous validation state and hide feedback
            inp.classList.remove('is-invalid');
            var fb = inp.parentNode.querySelector('.invalid-feedback');
            if (fb) fb.style.display = 'none';
            var v = inp.value;
            if (v === null || v === undefined || v === '') {
                payload.scores.push(null);
            } else {
                // allow only integer values
                var num = Number(v);
                var isInteger = Number.isInteger(num);
                var withinRange = (num >= 0 && num <= 9999999);
                if (!isInteger || !withinRange) {
                    invalidFound = true;
                    if (!firstInvalid) firstInvalid = inp;
                    inp.classList.add('is-invalid');
                    if (fb) fb.style.display = 'block';
                    payload.scores.push(null);
                } else {
                    payload.scores.push(num);
                }
            }
        });

        if (invalidFound) {
            if (firstInvalid && firstInvalid.focus) firstInvalid.focus();
            if (sideFeedback) { sideFeedback.style.display = 'block'; sideFeedback.textContent = 'Corrige los campos marcados antes de enviar.'; }
            return;
        }

        fetch('/api/scores', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(function(resp){
            if (resp.ok) {
                // clear form fields
                form.reset();
                // clear any field invalid state
                form.querySelectorAll('.is-invalid').forEach(function(i){ i.classList.remove('is-invalid'); });
                // show brief visual confirmation on the button
                var btn = form.querySelector('.btn');
                if (btn) {
                    var old = btn.textContent;
                    btn.textContent = 'Enviado';
                    setTimeout(function(){ btn.textContent = old; }, 1400);
                }
                if (sideFeedback) { sideFeedback.style.display = 'block'; sideFeedback.classList.remove('text-danger'); sideFeedback.classList.add('text-success'); sideFeedback.textContent = 'Puntuaciones enviadas correctamente.'; setTimeout(function(){ sideFeedback.style.display = 'none'; }, 2500); }
            } else {
                if (sideFeedback) { sideFeedback.style.display = 'block'; sideFeedback.classList.remove('text-success'); sideFeedback.classList.add('text-danger'); sideFeedback.textContent = 'Error al enviar las puntuaciones.'; }
            }
        }).catch(function(){
            if (sideFeedback) { sideFeedback.style.display = 'block'; sideFeedback.classList.remove('text-success'); sideFeedback.classList.add('text-danger'); sideFeedback.textContent = 'No se pudo conectar con el servidor.'; }
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        // Expose functions to global scope only as needed for inline handlers
        window.toggleSelection = toggleSelection;
        window.validateAndSubmit = validateAndSubmit;

            // intercept the sideForm submit to send via AJAX and clear the form
            var sideForm = document.getElementById('sideForm');
            if (sideForm) {
                sideForm.addEventListener('submit', submitScores);
            }

        updateInstruction();
        updateSelectionCount();
    });
})();
