# Section 2 — Explanatory paragraph for the system class diagram

> A plain-English paragraph to sit beneath Figure 1 in the report, explaining the relationships
> to someone who does not know SIFFT or the background. Matches the diagram at
> `sifft_config_rewrite/docs/sifft_rewrite_uml/SYSTEM_CLASS_DIAGRAM.drawio` (8 classes, 7
> relationships).

---

The diagram shows how the eight classes in the system are connected to one another. It uses four
types of relationship, each chosen to describe a different kind of link.

The two controllers (ReadConfigController and WriteConfigController) each have a permanent link
to their service (ReadConfigService and WriteConfigService). This is shown as an **association**
(a solid arrow labelled "uses"), because each controller always holds a reference to exactly one
service and relies on it for every request. The controllers themselves contain no business rules;
they simply pass requests to the service and return the response.

Each service keeps a collection of settings. ReadConfigService holds zero or more ReadConfig
objects, and WriteConfigService holds zero or more WriteConfig objects. This is shown as an
**aggregation** (an unfilled diamond at the service end, labelled "stores", with "0..*" at the
settings end), because the service groups the settings together and looks after them, but the
settings are ordinary objects that could in principle exist on their own. The "0..*" means the
service may hold any number, including none.

ReadConfig and WriteConfig both share an identifier (a unique reference number) that is defined
once in BaseConfig rather than being repeated in both classes. This is shown as
**generalisation / inheritance** (a hollow triangle pointing to BaseConfig), meaning "ReadConfig
is a kind of BaseConfig" and "WriteConfig is a kind of BaseConfig." BaseConfig is abstract — it
is never created on its own; it exists only so the two settings can inherit from it.

Finally, ReadConfigService has a link to ConfigTestResult, shown as a **dependency** (a dashed
arrow labelled "creates"). This is a brief, one-off use: the service creates a
ConfigTestResult only while running the test feature (which splits a sample line and reports
whether the field count matches), then returns it. It does not keep one permanently, which is
why this is a dashed dependency rather than a solid link.

---

## Multiplicity check note (for the writer)

> The two aggregation edges show "0..*" at the settings end (correct — any number of configs),
> but do not explicitly show "1" at the service (diamond) end. The lecture says that if no
> multiplicity is shown it is assumed to be 1, so this is technically correct — but adding an
> explicit "1" at the diamond end would be better for a "full and correct use of syntax" mark.
> Consider adding it in draw.io before exporting.
