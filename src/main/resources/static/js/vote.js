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

    document.addEventListener('DOMContentLoaded', function() {
        // Expose functions to global scope only as needed for inline handlers
        window.toggleSelection = toggleSelection;
        window.validateAndSubmit = validateAndSubmit;

        updateInstruction();
        updateSelectionCount();
    });
})();
