import { LitElement, css, html } from 'lit';

import './app-calendar.js';
import './app-photos.js';

const VIEWING_RANGES = [
  [[7, 0, 0, 0],[19, 0, 0, 0]],
];

navigator.serviceWorker.register('./sw.js')

class AppMain extends LitElement {
  static get properties() {
    return {
      interval: { type: Number }
    }
  }

  #intervalCounter = 0;

  constructor() {
    super();
    this.interval = 15_000;
  }

  firstUpdated() {
    this.#updateApp();
    this.#populatePhotos();
  }

  #updateApp() {
    const now = Date.now();
    if (this.#isViewingTime(now)) {
      if (this.#intervalCounter++ % 2) {
        this.shadowRoot.getElementById('photos').dispatchEvent(new CustomEvent('show-photo'));
        this.#show('photos');
      } else {
        this.#show('calendar');
      }
    } else {
      this.#show('blackout');
    }

    setTimeout(() => this.#updateApp(), this.interval);
  }

  #isViewingTime(date) {
    for (const [startTime, endTime] of VIEWING_RANGES) {
      const startDate = new Date(date);
      // @ts-ignore
      startDate.setHours(...startTime);
      
      const endDate = new Date(date);
      // @ts-ignore
      endDate.setHours(...endTime);
    
      if (date >= startDate && date < endDate) {
        return true;
      }
    }
    return false;
  }
  
  #show(id) {
    const containers = this.shadowRoot.querySelectorAll('.container');
    // @ts-ignore
    for (const container of containers) {
      if (container.classList.contains('retiring')) {
        container.classList.remove('retiring');
      }

      if (container.id === id) {
        container.classList.add('foreground');
      } else if (container.classList.contains('foreground')) {
        container.classList.remove('foreground');
        container.classList.add('retiring');
      }
    };
  }

  #populatePhotos() {
    try {
      console.log('populating photos');
      this.shadowRoot.getElementById('photos')
        .dispatchEvent(new CustomEvent('populate-photos'));
    } finally {
      const midnight = new Date().setHours(24, 0, 0, 0);
      setTimeout(() => {
        this.#populatePhotos();
      }, midnight - Date.now());
    }
  }

  static get styles() {
    return css`
      :host {
        width: 100%;
        height: 100%;
        display: block;
        overflow: hidden;
      }

      .container {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        overflow: hidden;

        z-index: 0;
        opacity: 0;
      }

      .retiring {
        opacity: 1;
      }

      @keyframes fade-in {
        from { opacity: 0; }
        20% { opacity: 0; }
        to { opacity: 1; }
      }

      .foreground {
        z-index: 1;
        opacity: 1;
        animation: fade-in 2s;
      }

      #blackout {
        background-color: black;
      }
    `;
  }

  render() {
    return html`
      <div id="blackout" class="container"></div>
      <app-photos id="photos" class="container"></app-photos>
      <app-calendar id="calendar" class="container"></app-calendar>
    `;
  }
}
customElements.define('app-main', AppMain);