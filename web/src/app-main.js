import { LitElement, css, html } from 'lit';

import { withGAPI } from './gapi.js';
import './app-calendar.js';

class AppMain extends LitElement {
  static get properties() {
    return {
      example: { attribute: null }
    }
  }

  constructor() {
    super();
    // withGAPI(async gapi => {
    //   return gapi.client.calendar.events.list({
    //     calendarId: 'primary',
    //     maxResults: 1,
    //     orderBy: 'startTime',
    //     singleEvents: true
    //   });
    // }).then(response => console.log(JSON.stringify(response.result, null, 2)));
  }

  firstUpdated() {
    this.#show('calendar');

    // const names = Array.from(
    //   this.shadowRoot.querySelectorAll('.container'),
    //   container => container.id);
    // setInterval(() => {
    //   const name = names.shift();
    //   names.push(name);
    //   console.log(name);
    //   this.#show(name);
    // }, 20_000);

    // this.#show(names[0]);
  }

  #show(id) {
    const containers = this.shadowRoot.querySelectorAll('.container');
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