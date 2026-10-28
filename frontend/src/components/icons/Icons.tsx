/* ----------------------------- Icons ------------------------------ */
// Ícones SVG usados na interface. Ficaram separados para não deixar o App.tsx gigante.
import React from 'react';

const IconLibrary = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <path d="M3 4.5h4.5v15H3zM9.5 4.5H14v15H9.5z" strokeLinejoin="round" />
    <path d="M16.2 5.3l3.9 1-3.5 14.6-3.9-1z" strokeLinejoin="round" />
  </svg>
);

const IconFeather = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <path d="M20 4c-7 1-13 6-15 16 10-2 15-8 16-15z" strokeLinejoin="round" />
    <path d="M9.5 14.5L5 19" />
  </svg>
);

const IconPeople = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <circle cx="9" cy="8" r="3" />
    <path d="M3.5 20c0-3.6 2.5-6 5.5-6s5.5 2.4 5.5 6" />
    <circle cx="17" cy="9" r="2.3" />
    <path d="M15.2 14.3c2.3.3 3.8 2.3 3.8 5.2" />
  </svg>
);

const IconGlobe = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <circle cx="12" cy="12" r="8.5" />
    <path d="M3.5 12h17M12 3.5c2.4 2.4 3.6 5.4 3.6 8.5s-1.2 6.1-3.6 8.5c-2.4-2.4-3.6-5.4-3.6-8.5S9.6 5.9 12 3.5z" />
  </svg>
);

const IconNote = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <path d="M6 3.5h9L19.5 8v12.5H6z" strokeLinejoin="round" />
    <path d="M14.5 3.5V8h5" strokeLinejoin="round" />
  </svg>
);

const IconCompass = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <circle cx="12" cy="12" r="8.5" />
    <path d="M14.8 9.2l-2 5.6-5.6 2 2-5.6z" strokeLinejoin="round" />
  </svg>
);

const IconGear = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <circle cx="12" cy="12" r="3" />
    <path d="M12 3.5v2.2M12 18.3v2.2M20.5 12h-2.2M5.7 12H3.5M18 6l-1.5 1.5M7.5 16.5L6 18M18 18l-1.5-1.5M7.5 7.5L6 6" />
  </svg>
);

const IconTrash = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <path d="M4.5 7h15M9.5 7V5h5v2M6.5 7l1 13h9l1-13" strokeLinejoin="round" />
  </svg>
);

const IconSearch = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...props}>
    <circle cx="10.8" cy="10.8" r="6.3" />
    <path d="M19.5 19.5l-4-4" strokeLinecap="round" />
  </svg>
);

const IconBell = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" {...props}>
    <path d="M6 10a6 6 0 1112 0c0 3.2 1 5 1.8 6H4.2C5 15 6 13.2 6 10z" strokeLinejoin="round" />
    <path d="M9.7 19a2.3 2.3 0 004.6 0" />
  </svg>
);

const IconChevron = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" {...props}>
    <path d="M7 10l5 5 5-5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconDots = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="currentColor" {...props}>
    <circle cx="6" cy="12" r="1.6" />
    <circle cx="12" cy="12" r="1.6" />
    <circle cx="18" cy="12" r="1.6" />
  </svg>
);

const IconStack = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
    <rect x="4" y="4.5" width="11" height="3.2" rx="0.6" />
    <rect x="4" y="9.7" width="16" height="3.2" rx="0.6" />
    <rect x="4" y="14.9" width="13" height="3.2" rx="0.6" />
  </svg>
);

const IconSparkle = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="currentColor" {...props}>
    <path d="M12 2c.6 3.6 2.4 5.4 6 6-3.6.6-5.4 2.4-6 6-.6-3.6-2.4-5.4-6-6 3.6-.6 5.4-2.4 6-6z" />
  </svg>
);

const IconPlus = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M12 5v14M5 12h14" strokeLinecap="round" />
  </svg>
);

const IconCollapse = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" {...props}>
    <path d="M15 5l-6 7 6 7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconClose = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M18 6L6 18M6 6l12 12" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconMenu = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M3 12h18M3 6h18M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconUndo = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M9 14L4 9l5-5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M4 9h11.5a5.5 5.5 0 015.5 5.5v0a5.5 5.5 0 01-5.5 5.5H11" strokeLinecap="round" />
  </svg>
);

const IconRedo = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M15 14l5-5-5-5" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M20 9H8.5A5.5 5.5 0 003 14.5v0A5.5 5.5 0 008.5 20H13" strokeLinecap="round" />
  </svg>
);

const IconAlignLeft = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M3 6h18M3 12h12M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconAlignCenter = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M3 6h18M6 12h12M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconAlignRight = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M3 6h18M9 12h12M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconAlignJustify = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M3 6h18M3 12h18M3 18h18" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconFocus = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M8 3H5a2 2 0 00-2 2v3m18 0V5a2 2 0 00-2-2h-3m0 18h3a2 2 0 002-2v-3M3 16v3a2 2 0 002 2h3" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconFileText = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconMinimize = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M8 3v3h-3m14 2h-3v-3M3 16h3v3m13-3h-3v3" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconChevronLeftDouble = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M13 17l-5-5 5-5M18 17l-5-5 5-5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconChevronRightDouble = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
    <path d="M11 17l5-5-5-5M6 17l5-5-5-5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const IconMessageSparkle = (props: React.SVGProps<SVGSVGElement>) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true" {...props}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z" />
    <path strokeLinecap="round" strokeLinejoin="round" d="M15 6l1.5 1.5L18 9l-1.5 1.5L15 12l-1.5-1.5L12 9l1.5-1.5z" />
  </svg>
);

export {
  IconLibrary,
  IconFeather,
  IconPeople,
  IconGlobe,
  IconNote,
  IconCompass,
  IconGear,
  IconTrash,
  IconSearch,
  IconBell,
  IconChevron,
  IconDots,
  IconStack,
  IconSparkle,
  IconPlus,
  IconCollapse,
  IconClose,
  IconMenu,
  IconUndo,
  IconRedo,
  IconAlignLeft,
  IconAlignCenter,
  IconAlignRight,
  IconAlignJustify,
  IconFocus,
  IconMinimize,
  IconFileText,
  IconChevronLeftDouble,
  IconChevronRightDouble,
  IconMessageSparkle
};
