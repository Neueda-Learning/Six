// Deutsche Sprachdatei
export default {
  app: {
    title: 'Zahlungsverarbeitungssystem',
    navList: 'Zahlungen',
    navCreate: 'Erstellen',
    navTrash: 'Zuletzt gelöscht'
  },
  language: {
    label: 'Sprache',
    zh: '中文',
    en: 'English',
    de: 'Deutsch'
  },
  list: {
    title: 'Zahlungsliste',
    newPayment: 'Neue Zahlung',
    status: 'Status',
    allStatuses: 'Alle Status',
    keyword: 'Stichwort',
    keywordPlaceholder: 'Nach Zahlungs-ID oder Bemerkung suchen',
    search: 'Suchen',
    reset: 'Zurücksetzen',
    moveToTrash: 'In Papierkorb',
    moveToTrashSuccess: 'Datensatz wurde in den Papierkorb verschoben',
    columns: {
      index: '#',
      paymentId: 'Zahlungs-ID',
      fromAccount: 'Von Konto',
      toAccount: 'An Konto',
      amount: 'Betrag',
      status: 'Status',
      remark: 'Bemerkung',
      createdAt: 'Erstellt am',
      actions: 'Aktionen'
    },
    empty: 'Keine Zahlungen gefunden'
  },
  trash: {
    title: 'Zuletzt gelöscht',
    keyword: 'Stichwort',
    keywordPlaceholder: 'Nach Zahlungs-ID oder Bemerkung suchen',
    search: 'Suchen',
    reset: 'Zurücksetzen',
    empty: 'Keine Einträge im Papierkorb',
    restore: 'Wiederherstellen',
    restoreSuccess: 'Datensatz wurde wiederhergestellt',
    confirmDelete: 'Endgültig löschen',
    confirmDeleteSuccess: 'Datensatz wurde dauerhaft gelöscht und kann nicht wiederhergestellt werden',
    confirmDeletePrompt: 'Diesen Datensatz dauerhaft löschen? Er kann danach nicht wiederhergestellt werden, bleibt aber in der Datenbank erhalten.',
    cancel: 'Abbrechen',
    retentionHint: 'Papierkorb-Einträge werden standardmäßig 30 Tage angezeigt. Danach bleiben sie in der Datenbank erhalten, werden aber im UI nicht mehr angezeigt.',
    columns: {
      paymentId: 'Zahlungs-ID',
      amount: 'Betrag',
      status: 'Status',
      remark: 'Bemerkung',
      deletedAt: 'Gelöscht am',
      recoverableUntil: 'Wiederherstellbar bis',
      actions: 'Aktionen'
    }
  },
  create: {
    title: 'Zahlung erstellen',
    fromAccount: 'Von Konto',
    fromAccountPlaceholder: 'z. B. ACC10001',
    toAccount: 'An Konto',
    toAccountPlaceholder: 'z. B. ACC20002',
    amount: 'Betrag',
    currency: 'Währung',
    currencyPlaceholder: 'Währung auswählen',
    remark: 'Bemerkung',
    remarkPlaceholder: 'Optionale Notiz, z. B. invoice-2026-07',
    idempotencyKey: 'Idempotenzschlüssel',
    regenerate: 'Neu generieren',
    submit: 'Zahlung absenden',
    reset: 'Zurücksetzen',
    submitSuccess: 'Zahlung erfolgreich übermittelt',
    validation: {
      fromRequired: 'Von Konto ist erforderlich',
      toRequired: 'An Konto ist erforderlich',
      toDifferent: 'An Konto muss sich von Von Konto unterscheiden',
      amountRequired: 'Betrag ist erforderlich',
      amountRange: 'Der Betrag muss zwischen 0,01 und 1.000.000 liegen',
      currencyRequired: 'Währung ist erforderlich',
      idempotencyRequired: 'Idempotenzschlüssel ist erforderlich'
    }
  },
  detail: {
    title: 'Zahlungsdetails',
    basicInfo: 'Grundinformationen',
    refreshStatus: 'Status aktualisieren',
    autoRefreshing: 'Status wird automatisch aktualisiert',
    paymentId: 'Zahlungs-ID',
    idempotencyKey: 'Idempotenzschlüssel',
    fromAccount: 'Von Konto',
    toAccount: 'An Konto',
    amount: 'Betrag',
    currency: 'Währung',
    remark: 'Bemerkung',
    createdAt: 'Erstellt am',
    updatedAt: 'Aktualisiert am',
    errorCode: 'Fehlercode',
    unknown: 'UNBEKANNT',
    noErrorMessage: 'Keine Fehlermeldung angegeben',
    statusHistory: 'Statusverlauf',
    start: 'START'
  },
  http: {
    requestFailed: 'Anfrage fehlgeschlagen',
    networkError: 'Netzwerkfehler bei der Anfrage'
  }
};
