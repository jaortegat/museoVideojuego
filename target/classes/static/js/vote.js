(function () {
    // MAX_VOTES will be provided by the server as a global variable in the template
    var MAX_VOTES = window.MAX_VOTES || 3;
    var selectedConsoles = new Set();

    function updateInstruction() {
        var instr = document.getElementById('maxVotesInstruction');
        if (instr) instr.textContent = 'Selecciona hasta ' + MAX_VOTES + ' consola(s).';
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
            alert('Solo puedes seleccionar hasta ' + MAX_VOTES + ' consola(s).');
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
            alert('Solo puedes seleccionar hasta ' + MAX_VOTES + ' consola(s).');
            return;
        }
        var input = document.getElementById('selectedConsoles');
        if (input) input.value = Array.from(selectedConsoles).join(',');
        var form = document.getElementById('voteForm');
        if (form) form.submit();
    }

    document.addEventListener('DOMContentLoaded', function() {
        // Expose functions to global scope only as needed for inline handlers
        window.toggleSelection = toggleSelection;
        window.validateAndSubmit = validateAndSubmit;

        updateInstruction();
        updateSelectionCount();
    });
})();
