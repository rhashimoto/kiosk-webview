import { LitElement, css, html } from 'lit';
import { repeat } from 'lit/directives/repeat.js';

import './app-calendar-event.js';
import { withGAPI } from './gapi.js';

/** @type {Intl.DateTimeFormatOptions} */
const DATE_HEADER_FORMAT = { weekday: 'long', month: 'short', day: 'numeric' }
/** @type {Intl.DateTimeFormatOptions} */
const TIME_HEADER_FORMAT = { timeStyle: 'short' };

const CALENDAR_POLL_INTERVAL = 10 * 60 * 1000;
const CALENDAR_WINDOW_SIZE = 7 * 24 * 60 * 60 * 1000;

class AppCalendar extends LitElement {
  static get properties() {
    return {
      dateHeader: { state: true },
      timeHeader: { state: true },
      calendars: { attribute: null },
      events: { state: true }
    }
  }

  constructor() {
    super();
    this.dateHeader = '';
    this.timeHeader = '';
    this.calendars = [];
    this.events = [];

    this.#updateDate();
    this.#updateEvents();
  }

  #updateDate() {
    const date = new Date();
    this.dateHeader = date.toLocaleDateString(undefined, DATE_HEADER_FORMAT);
    this.timeHeader = date.toLocaleTimeString(undefined, TIME_HEADER_FORMAT).toLowerCase();
    setTimeout(() => this.#updateDate(), new Date().setSeconds(60, 0) - Date.now());
  }

  async #updateEvents() {
    try {
      await this.#updateEventsHelper();
    } finally {
      setTimeout(() => this.#updateEvents(), CALENDAR_POLL_INTERVAL);
    }
  }

  async #updateEventsHelper() {
    await this.#loadCalendarListIfNeeded();
    const events = await withGAPI(async gapi => {
      // Fetch calendar events.
      const startTime = new Date().setHours(0, 0, 0, 0);
      const endTime = startTime + CALENDAR_WINDOW_SIZE;
      return Promise.all(this.calendars.map(async calendar => {
        const response = await gapi.client.calendar.events.list({
          calendarId: calendar.id,
          orderBy: 'startTime',
          singleEvents: true,
          timeMin: new Date(startTime).toISOString(),
          timeMax: new Date(endTime).toISOString()
        });
        return response.result.items;
      }));
    });

    this.events = events
      .flat()
      .sort((a, b) => {
        // Order by start time.
        const aTime = a.start.dateTime || a.start.date;
        const bTime = b.start.dateTime || b.start.date;
        return aTime.localeCompare(bTime)
      })
      .filter((() => {
        // Remove recurring events after the first instance.
        const recurring = new Set();
        return event => {
          if (recurring.has(event.recurringEventId)) {
            return false;
          }
          recurring.add(event.recurringEventId || '');
          return true;
        }
      })())
      .filter((_, i) => i < 9);
  }

  #loadCalendarListIfNeeded() {
    if (this.calendars.length === 0) {
      return withGAPI(async gapi => {
        // Get the calendars of interest.
        this.calendars = await gapi.client.calendar.calendarList.list({}).then(response => {
          return response.result.items.filter(calendar => {
            return calendar.selected &&
                   ['owner', 'writer'].includes(calendar.accessRole);
          });
        });
      });
    }
  }

  static get styles() {
    return css`
      :host {
        background-color: black;
        color: white;

        display: flex;
        flex-direction: column;
      }

      .header {
        display: flex;
        flex-direction: row;
        justify-content: space-between;

        font-size: 7.5vw;
        padding: 1rem;
      }

      .content {
        flex-grow: 1;

        display: grid;
        grid-template-columns: repeat(3, 1fr);
        grid-template-rows: repeat(3, 1fr);
        grid-auto-columns: 0;
        grid-auto-rows: 0;
        grid-auto-flow: column;
        gap: 0.5rem;
      }

      .content > * {
        min-width: 0;
        min-height: 0;
      }
    `;
  }

  render() {
    return html`
      <div class="header">
        <div>${this.dateHeader}</div>
        <div>${this.timeHeader}</div>
      </div>
      <div class="content">
        ${repeat(this.events, event => html`
          <app-calendar-event .event=${event}></app-calendar-event>
        `)}
      </div>
    `;
  }
}
customElements.define('app-calendar', AppCalendar);