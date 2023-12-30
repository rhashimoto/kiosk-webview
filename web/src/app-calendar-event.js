import { LitElement, css, html } from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';

const COLOR_TODAY = 'gold';
const COLOR_TOMORROW = 'lightgreen';

class AppCalendarEvent extends LitElement {
  static get properties() {
    return {
      event: { attribute: null }
    }
  }

  constructor() {
    super();
    this.event = {};
  }

  #getDescription() {
    const description = ((description) => {
      if (description.startsWith('<html-blob>')) {
        return new DOMParser()
          .parseFromString(this.event.description, 'text/xml')
          .documentElement
          .textContent;
      }
      return description;
    })(this.event.description ?? '');

    const html = new DOMParser()
      .parseFromString(description, 'text/html')
      .body
      .innerHTML;
    return unsafeHTML(html);
  }

  #getDate() {
    const date = new Date(this.event.start.dateTime || (this.event.start.date + 'T00:00'));
    const today = new Date().setHours(0, 0, 0, 0);
    switch(Math.trunc((date.valueOf() - today) / 86_400_000)) {
      case 0:
        this.style.backgroundColor = COLOR_TODAY;
        return 'Today';
      case 1:
        this.style.backgroundColor = COLOR_TOMORROW;
        return 'Tomorrow';
      default:
        this.style.backgroundColor = null;
        return date.toLocaleDateString(undefined, {
          weekday: 'long',
          month: 'short',
          day: 'numeric'
        });
    }
  }

  #getTime() {
    const date = new Date(this.event.start.dateTime || (this.event.start.date + 'T00:00'));
    return date.toLocaleTimeString(undefined, { timeStyle: 'short' });

  }

  static get styles() {
    return css`
      :host {
        color: black;
        background-color: lightblue;
        font-size: 2vw;
        padding-left: 0.25em;
        padding-right: 0.25em;

        display: flex;
        flex-direction: column;        
      }

      .today {
        background-color: gold;
      }

      .tomorrow {
        background-color: lightgreen;
      }

      .summary {
        font-weight: bold;
        font-size: 2.5vw;
        padding-bottom: 0.5em;
      }

      .description {
        flex-grow: 1;
      }

      .footer {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
      }
    `;
  }

  render() {
    return html`
      <div class="summary">${this.event.summary}</div>
      <div class="description">${this.#getDescription()}</div>
      <div class="footer">
        <div class="date">${this.#getDate()}</div>
        <div class="time">${this.#getTime()}</div>
      </div>
    `;
  }
}
customElements.define('app-calendar-event', AppCalendarEvent);