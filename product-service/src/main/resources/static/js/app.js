(function () {
    'use strict';

    function pad(value) {
        return value < 10 ? '0' + value : String(value);
    }

    // Same format as the server renders: "01:02:03", or "2d 01:02:03" past a day
    function formatUptime(totalSeconds) {
        var days = Math.floor(totalSeconds / 86400);
        var hours = Math.floor((totalSeconds % 86400) / 3600);
        var minutes = Math.floor((totalSeconds % 3600) / 60);
        var seconds = totalSeconds % 60;
        var clock = pad(hours) + ':' + pad(minutes) + ':' + pad(seconds);
        return days > 0 ? days + 'd ' + clock : clock;
    }

    document.querySelectorAll('[data-started-at]').forEach(function (element) {
        var startedAt = Number(element.getAttribute('data-started-at'));
        if (!startedAt) {
            return;
        }
        setInterval(function () {
            var elapsed = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
            element.textContent = formatUptime(elapsed);
        }, 1000);
    });

    document.addEventListener('click', function (event) {
        var target = event.target.closest('[data-copy]');
        if (!target || !navigator.clipboard) {
            return;
        }
        navigator.clipboard.writeText(target.textContent.trim()).then(function () {
            target.classList.add('copied');
            setTimeout(function () {
                target.classList.remove('copied');
            }, 1200);
        });
    });
})();
