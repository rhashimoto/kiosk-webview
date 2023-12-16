navigator.serviceWorker.register('./sw.js');
navigator.wakeLock.request('screen');

window.addEventListener('load', () => {
  // Watch for changes to the container element contents.
  const container = document.querySelector('[data-container]');
  const observer = new MutationObserver(mutations => {
    for (const mutation of mutations) {
      if (mutation.type === 'childList') {
        for (const node of mutation.addedNodes) {
          if (node instanceof HTMLElement && node.parentElement === container) {
            // Use CSS transform to scale the element to fit the container.
            node.style.padding = '0';
            node.style.margin = '0';
            const scaleX = container.clientWidth / node.offsetWidth;
            const scaleY = 0.95 * container.clientHeight / node.offsetHeight;
            const scale = Math.min(scaleX, scaleY);
            const transform = `scale(${scale})`;
            node.style.transform = transform;
          }
        }
      }
    }
  });
  observer.observe(container, { childList: true });
});