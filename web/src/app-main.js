import { LitElement, css, html } from 'lit';
import { ref, createRef } from 'lit/directives/ref.js';

const ANDROID_WEBVIEW = 'https://appassets.androidplatform.net';

class AppMain extends LitElement {
  static get properties() {
    return {
      example: { attribute: null }
    }
  }

  constructor() {
    super();
    if (window.location.href.startsWith(ANDROID_WEBVIEW)) {
      fetch(`${ANDROID_WEBVIEW}/x/clientId`).then(async response => {
        const clientId = await response.text();
        console.log(clientId);
      });
      fetch(`${ANDROID_WEBVIEW}/x/accessToken`).then(async response => {
        const accessToken = await response.text();
        console.log(accessToken);
      });
    }
  }

  firstUpdated() {
    const names = Array.from(
      this.shadowRoot.querySelectorAll('.container'),
      container => container.id);
    setInterval(() => {
      const name = names.shift();
      names.push(name);
      console.log(name);
      this.#show(name);
    }, 20_000);

    this.#show(names[0]);
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
      <div id="red" class="container" style="background-color: red;"></div>
      <div id="green" class="container" style="background-color: green;"></div>
      <div id="blue" class="container" style="background-color: blue;"></div>
    `;
  }
}
customElements.define('app-main', AppMain);