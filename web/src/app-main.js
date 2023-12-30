import { LitElement, css, html } from 'lit';

import './app-calendar.js';

const UPDATE_INTERVAL = 60_000;
const DAYLIGHT_RANGES = [
  [[7, 0, 0, 0],[19, 0, 0, 0]],
];

class AppMain extends LitElement {
  static get properties() {
    return {
      example: { attribute: null }
    }
  }

  constructor() {
    super();
  }

  firstUpdated() {
    this.#updateApp();
  }

  #updateApp() {
    if (this.#isDaylight(new Date())) {
      this.#show('calendar');
    } else {
      this.#show('blackout');
    }

    setTimeout(() => this.#updateApp(), UPDATE_INTERVAL);
  }

  #isDaylight(date) {
    for (const [startTime, endTime] of DAYLIGHT_RANGES) {
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
        to { opacity: 1; }
      }

      .foreground {
        z-index: 1;
        opacity: 1;
        animation: fade-in 1s;
      }

      #blackout {
        background-color: black;
      }
    `;
  }

  render() {
    return html`
      <div id="blackout" class="container"></div>
      <app-calendar id="calendar" class="container"></app-calendar>
    `;
  }
}
customElements.define('app-main', AppMain);