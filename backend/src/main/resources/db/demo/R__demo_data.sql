-- ============================================================================
-- DEMO DATA — local development only.
-- Flyway re-runs this file whenever it changes, and it starts by wiping the
-- business tables, so anything created through the app is reset too.
-- Users (app_user, login_event) and the audit trail are never touched.
-- Prices are sample values for demonstration, not market data.
-- ============================================================================

TRUNCATE message, report_job, proposal_line, proposal, trade_order,
         mifid_answer, mifid_profile, valuation_history, position, cash_account,
         portfolio, client, instrument RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------- instruments
INSERT INTO instrument (id, code, name, asset_class, currency, exchange, risk_class, complex, last_price, price_date) VALUES
 (1,  'NL0010273215', 'ASML Holding',                      'EQUITY', 'EUR', 'Euronext Amsterdam', 6, FALSE,  702.40, '2026-10-16'),
 (2,  'DE0007164600', 'SAP',                               'EQUITY', 'EUR', 'Xetra',              5, FALSE,  231.85, '2026-10-16'),
 (3,  'FR0000121014', 'LVMH Moët Hennessy Louis Vuitton',  'EQUITY', 'EUR', 'Euronext Paris',     5, FALSE,  604.20, '2026-10-16'),
 (4,  'DE0007236101', 'Siemens',                           'EQUITY', 'EUR', 'Xetra',              5, FALSE,  212.30, '2026-10-16'),
 (5,  'FR0000120271', 'TotalEnergies',                     'EQUITY', 'EUR', 'Euronext Paris',     5, FALSE,   55.62, '2026-10-16'),
 (6,  'DE0008404005', 'Allianz',                           'EQUITY', 'EUR', 'Xetra',              5, FALSE,  352.10, '2026-10-16'),
 (7,  'NL0000235190', 'Airbus',                            'EQUITY', 'EUR', 'Euronext Paris',     5, FALSE,  191.48, '2026-10-16'),
 (8,  'FR0000120321', 'L''Oréal',                          'EQUITY', 'EUR', 'Euronext Paris',     5, FALSE,  383.75, '2026-10-16'),
 (9,  'DE0005557508', 'Deutsche Telekom',                  'EQUITY', 'EUR', 'Xetra',              4, FALSE,   30.14, '2026-10-16'),
 (10, 'IE00B4L5Y983', 'iShares Core MSCI World UCITS ETF', 'ETF',    'EUR', 'Xetra',              4, FALSE,  104.86, '2026-10-16'),
 (11, 'IE00BK5BQT80', 'Vanguard FTSE All-World UCITS ETF', 'ETF',    'EUR', 'Xetra',              4, FALSE,  134.92, '2026-10-16'),
 (12, 'IE00B4WXJJ64', 'iShares Core EUR Govt Bond UCITS ETF', 'ETF', 'EUR', 'Xetra',              2, FALSE,  114.37, '2026-10-16'),
 (13, 'DEMO-FUND-EQ01', 'Demo European Equity Income Fund', 'FUND',  'EUR', NULL,                 4, FALSE,   48.21, '2026-10-16'),
 (14, 'DEMO-FUND-BD01', 'Demo Euro Short Duration Bond Fund', 'FUND','EUR', NULL,                 2, FALSE,   10.94, '2026-10-16'),
 (15, 'DEMO-BOND-DE34', 'Demo Federal Republic 2.5% 2034',  'BOND',  'EUR', NULL,                 2, FALSE,   98.45, '2026-10-16'),
 (16, 'DEMO-BOND-FR31', 'Demo French Republic 3.0% 2031',   'BOND',  'EUR', NULL,                 2, FALSE,  100.82, '2026-10-16'),
 (17, 'DEMO-BOND-CORP29', 'Demo Industrial Corp 4.25% 2029', 'BOND', 'EUR', NULL,                 3, FALSE,  101.60, '2026-10-16');

-- Derivatives (price per unit of underlying; value = price x contract size x contracts)
INSERT INTO instrument (id, code, name, asset_class, currency, exchange, risk_class, complex, last_price, price_date,
                        underlying_id, option_type, strike, expiry, contract_size) VALUES
 (18, 'DEMO-ASML-C750-DEC26', 'ASML Call 750 Dec 2026',      'OPTION', 'EUR', 'Eurex', 7, TRUE,  18.40, '2026-10-16', 1,    'CALL', 750,  '2026-12-18', 100),
 (19, 'DEMO-ASML-P650-DEC26', 'ASML Put 650 Dec 2026',       'OPTION', 'EUR', 'Eurex', 7, TRUE,  14.10, '2026-10-16', 1,    'PUT',  650,  '2026-12-18', 100),
 (20, 'DEMO-SAP-C240-MAR27',  'SAP Call 240 Mar 2027',       'OPTION', 'EUR', 'Eurex', 7, TRUE,   9.75, '2026-10-16', 2,    'CALL', 240,  '2027-03-19', 100),
 (21, 'DEMO-FESX-DEC26',      'Euro Stoxx 50 Future Dec 2026','FUTURE','EUR', 'Eurex', 7, TRUE, 5412.00, '2026-10-16', NULL, NULL,  NULL, '2026-12-18', 10);

-- ---------------------------------------------------------------- clients
-- rm1 manages clients 1-7; client 8 belongs to another RM (rm2) to show
-- that each RM only sees their own book. client1 logs in to the portal.
INSERT INTO client (id, client_number, first_name, last_name, email, phone, birth_date, country, category, rm_username, portal_username) VALUES
 (1, 'C-100231', 'Clara',  'Dupont',    'clara.dupont@example.com',  '+33 6 12 34 56 78', '1981-04-12', 'FR', 'RETAIL',       'rm1', 'client1'),
 (2, 'C-100232', 'Hans',   'Becker',    'hans.becker@example.com',   '+49 151 2345 678',  '1969-09-30', 'DE', 'PROFESSIONAL', 'rm1', NULL),
 (3, 'C-100233', 'Sofia',  'Rossi',     'sofia.rossi@example.com',   '+39 347 123 4567',  '1990-01-22', 'IT', 'RETAIL',       'rm1', NULL),
 (4, 'C-100234', 'Liam',   'O''Connor', 'liam.oconnor@example.com',  '+353 87 123 4567',  '1985-06-03', 'IE', 'RETAIL',       'rm1', NULL),
 (5, 'C-100235', 'Amélie', 'Laurent',   'amelie.laurent@example.com','+33 7 98 76 54 32', '1976-11-18', 'FR', 'RETAIL',       'rm1', NULL),
 (6, 'C-100236', 'Pieter', 'de Vries',  'pieter.devries@example.com','+31 6 1234 5678',   '1972-02-14', 'NL', 'PROFESSIONAL', 'rm1', NULL),
 (7, 'C-100237', 'Elena',  'García',    'elena.garcia@example.com',  '+34 612 345 678',   '1994-08-09', 'ES', 'RETAIL',       'rm1', NULL),
 (8, 'C-100238', 'Noah',   'Weber',     'noah.weber@example.com',    '+41 79 123 45 67',  '1966-12-01', 'CH', 'PROFESSIONAL', 'rm2', NULL);

-- ---------------------------------------------------------------- portfolios
INSERT INTO portfolio (id, portfolio_number, name, client_id, service_type, currency, opened_on) VALUES
 (1, 'P-2001', 'Balanced growth',     1, 'ADVISORY',       'EUR', '2021-03-01'),
 (2, 'P-2002', 'Trading account',     1, 'EXECUTION_ONLY', 'EUR', '2023-06-15'),
 (3, 'P-2003', 'Managed growth',      2, 'DISCRETIONARY',  'EUR', '2018-01-10'),
 (4, 'P-2004', 'Income portfolio',    3, 'ADVISORY',       'EUR', '2022-09-05'),
 (5, 'P-2005', 'Self-directed',       4, 'EXECUTION_ONLY', 'EUR', '2024-02-20'),
 (6, 'P-2006', 'Family savings',      5, 'ADVISORY',       'EUR', '2020-05-12'),
 (7, 'P-2007', 'Opportunities',       6, 'ADVISORY',       'EUR', '2019-11-01'),
 (8, 'P-2008', 'First investments',   7, 'ADVISORY',       'EUR', '2025-01-08'),
 (9, 'P-2009', 'Swiss mandate',       8, 'DISCRETIONARY',  'EUR', '2017-04-03');

INSERT INTO cash_account (portfolio_id, currency, balance) VALUES
 (1, 'EUR',  18450.00),
 (2, 'EUR',   6230.55),
 (3, 'EUR',  42100.00),
 (4, 'EUR',  12780.40),
 (5, 'EUR',   3105.00),
 (6, 'EUR', 148900.00),   -- high cash ratio, shows on the RM homepage
 (7, 'EUR',  65400.00),
 (8, 'EUR',   9800.00),
 (9, 'EUR',  88000.00);

-- ---------------------------------------------------------------- positions
-- Bonds: quantity = nominal, prices in % of nominal.
INSERT INTO position (portfolio_id, instrument_id, quantity, average_cost) VALUES
 -- 1 Clara, balanced
 (1, 10,   420, 88.10), (1, 12, 300, 112.40), (1, 2, 110, 182.60), (1, 8, 45, 352.00),
 (1, 15, 40000, 97.20), (1, 13, 800, 44.90),
 -- 2 Clara, trading
 (2, 1,     12, 615.00), (2, 7, 40, 165.20), (2, 9, 300, 26.80),
 -- 3 Hans, discretionary
 (3, 11,   900, 112.30), (3, 1, 60, 540.00), (3, 3, 50, 655.00), (3, 4, 180, 176.50),
 (3, 6,    120, 290.40), (3, 16, 75000, 99.10), (3, 17, 50000, 100.20),
 -- 4 Sofia, income
 (4, 14,  6000, 10.70), (4, 12, 260, 115.90), (4, 5, 400, 58.30), (4, 15, 25000, 99.00),
 -- 5 Liam, self-directed
 (5, 10,   150, 96.40), (5, 2, 30, 205.10),
 -- 6 Amélie, mostly cash
 (6, 11,   220, 120.00), (6, 9, 900, 27.40),
 -- 7 Pieter, opportunities with derivatives
 (7, 1,     90, 580.00), (7, 2, 200, 190.00), (7, 4, 150, 188.00), (7, 7, 200, 172.00),
 (7, 18,     5, 21.00), (7, 20, 8, 8.30), (7, 21, 2, 5280.00),
 -- 8 Elena, starting out
 (8, 10,    60, 98.00), (8, 13, 150, 46.20),
 -- 9 Noah (rm2)
 (9, 11,   1200, 109.00), (9, 3, 80, 640.00), (9, 16, 120000, 100.40);

-- ---------------------------------------------------------------- history
-- Month-end performance index (100 = 31 Oct 2025), a gentle trend plus a wave.
INSERT INTO valuation_history (portfolio_id, as_of, index_value)
SELECT p.id,
       (date '2025-10-31' + (n || ' month')::interval)::date,
       round((100 * (1 + n * p.drift + 0.022 * sin(n * 0.9 + p.id)) - 2.2 * sin(p.id))::numeric, 4)
FROM (VALUES (1, 0.0060), (2, 0.0090), (3, 0.0075), (4, 0.0030), (5, 0.0080),
             (6, 0.0020), (7, 0.0110), (8, 0.0050), (9, 0.0065)) AS p (id, drift),
     generate_series(0, 11) AS n;

-- ---------------------------------------------------------------- MiFID
INSERT INTO mifid_profile (id, client_id, questionnaire_version, status, risk_score, complex_products_ok,
                           completion_percent, client_decision, valid_until, created_by, created_at, updated_at, completed_at) VALUES
 (1, 1, '2026.1', 'VALID',              3, FALSE, 100, 'ACCEPTED',     '2027-03-14', 'rm1', '2026-03-14 10:00+01', '2026-03-14 10:25+01', '2026-03-14 10:25+01'),
 (2, 2, '2026.1', 'VALID',              4, TRUE,  100, 'ACCEPTED',     '2027-05-02', 'rm1', '2026-05-02 14:00+02', '2026-05-02 14:30+02', '2026-05-02 14:30+02'),
 (3, 3, '2026.1', 'VALID',              2, FALSE, 100, 'ACCEPTED',     '2026-08-31', 'rm1', '2025-08-31 09:00+02', '2025-08-31 09:20+02', '2025-08-31 09:20+02'),
 (4, 5, '2026.1', 'PENDING_VALIDATION', 3, FALSE, 100, 'UNDER_REVIEW', NULL,         'rm1', '2026-10-12 11:00+02', '2026-10-12 11:40+02', NULL),
 (5, 6, '2026.1', 'VALID',              5, TRUE,  100, 'ACCEPTED',     '2027-01-20', 'rm1', '2026-01-20 16:00+01', '2026-01-20 16:20+01', '2026-01-20 16:20+01'),
 (6, 7, '2026.1', 'IN_PROGRESS',        NULL, FALSE, 20, NULL,         NULL,         'rm1', '2026-10-15 15:00+02', '2026-10-15 15:05+02', NULL),
 (7, 8, '2026.1', 'VALID',              4, TRUE,  100, 'ACCEPTED',     '2027-04-11', 'rm2', '2026-04-11 10:00+02', '2026-04-11 10:30+02', '2026-04-11 10:30+02');
-- Client 3 (Sofia) has an expired profile; client 4 (Liam) has none.

INSERT INTO mifid_answer (profile_id, question_id, option_id) VALUES
 -- Clara: score 19 -> 3 Balanced
 (1,'goal','growth'),(1,'horizon','y5to10'),(1,'income','50to100'),(1,'share','25to50'),(1,'reserve','3to6m'),
 (1,'drawdown','hold'),(1,'maxloss','l10'),(1,'exp_equities','some'),(1,'exp_derivatives','none'),(1,'leverage_quiz','unsure'),
 -- Hans: score 23 -> 4 Growth, complex products OK
 (2,'goal','growth'),(2,'horizon','gt10'),(2,'income','100to250'),(2,'share','lt25'),(2,'reserve','gt6m'),
 (2,'drawdown','hold'),(2,'maxloss','l10'),(2,'exp_equities','regular'),(2,'exp_derivatives','regular'),(2,'leverage_quiz','false'),
 -- Sofia: score 12 -> 2 Cautious
 (3,'goal','income'),(3,'horizon','y2to5'),(3,'income','lt50'),(3,'share','50to75'),(3,'reserve','lt3m'),
 (3,'drawdown','sellpart'),(3,'maxloss','l5'),(3,'exp_equities','some'),(3,'exp_derivatives','none'),(3,'leverage_quiz','unsure'),
 -- Amélie: score 19 -> 3, awaiting the client's decision
 (4,'goal','growth'),(4,'horizon','y5to10'),(4,'income','50to100'),(4,'share','25to50'),(4,'reserve','3to6m'),
 (4,'drawdown','hold'),(4,'maxloss','l10'),(4,'exp_equities','some'),(4,'exp_derivatives','none'),(4,'leverage_quiz','unsure'),
 -- Pieter: score 28 -> 5 Dynamic, complex products OK
 (5,'goal','speculation'),(5,'horizon','gt10'),(5,'income','gt250'),(5,'share','lt25'),(5,'reserve','gt6m'),
 (5,'drawdown','buymore'),(5,'maxloss','l30'),(5,'exp_equities','regular'),(5,'exp_derivatives','regular'),(5,'leverage_quiz','false'),
 -- Elena: started, objectives section only
 (6,'goal','growth'),(6,'horizon','gt10'),
 -- Noah (rm2)
 (7,'goal','growth'),(7,'horizon','gt10'),(7,'income','gt250'),(7,'share','lt25'),(7,'reserve','gt6m'),
 (7,'drawdown','hold'),(7,'maxloss','l5'),(7,'exp_equities','regular'),(7,'exp_derivatives','some'),(7,'leverage_quiz','false');

-- ---------------------------------------------------------------- orders
INSERT INTO trade_order (id, portfolio_id, instrument_id, side, quantity, limit_type, limit_price, stop_price,
                         time_in_force, valid_until, status, estimated_price, gross_amount, fees, net_amount,
                         negotiated_fee, executed_price, executed_at, check_summary, proposal_id,
                         created_by, created_at, decided_by, decided_at, decision_note) VALUES
 (1, 1, 2,  'BUY',  40,  'MARKET', NULL,  NULL, 'DAY', NULL, 'FILLED', 214.10,  8564.00, 21.41,  8585.41, NULL, 214.10, '2026-07-08 10:12+02', NULL, NULL, 'rm1', '2026-07-08 10:11+02', NULL, NULL, NULL),
 (2, 1, 15, 'BUY',  15000,'LIMIT', 97.50, NULL, 'GTC', NULL, 'FILLED', 97.40,  14610.00, 36.53, 14646.53, NULL, 97.40, '2026-08-21 14:02+02', NULL, NULL, 'rm1', '2026-08-20 09:30+02', NULL, NULL, NULL),
 (3, 2, 1,  'BUY',  4,   'MARKET', NULL,  NULL, 'DAY', NULL, 'FILLED', 668.00,  2672.00, 15.00,  2687.00, NULL, 668.00, '2026-09-03 11:45+02', NULL, NULL, 'client1', '2026-09-03 11:45+02', NULL, NULL, NULL),
 (4, 2, 7,  'SELL', 10,  'LIMIT', 205.00, NULL, 'GTC', NULL, 'PLACED', 191.48,  2050.00, 15.00,  2035.00, NULL, NULL, NULL, NULL, NULL, 'client1', '2026-10-14 16:20+02', NULL, NULL, NULL),
 (5, 7, 21, 'BUY',  1,   'MARKET', NULL,  NULL, 'DAY', NULL, 'FILLED', 5240.00, 52400.00, 15.00, 52415.00, NULL, 5240.00, '2026-06-12 09:05+02', NULL, NULL, 'rm1', '2026-06-12 09:04+02', NULL, NULL, NULL),
 (6, 7, 1,  'BUY',  40,  'MARKET', NULL,  NULL, 'DAY', NULL, 'PENDING_APPROVAL', 702.40, 28096.00, 70.24, 28166.24, NULL, NULL, NULL,
     'Concentration: ASML Holding would be 25.3% of the portfolio (limit 20%)', NULL, 'rm1', '2026-10-16 15:40+02', NULL, NULL, NULL),
 (7, 4, 5,  'BUY',  100, 'MARKET', NULL,  NULL, 'DAY', NULL, 'FILLED', 57.10,   5710.00, 15.00,  5725.00, NULL, 57.10, '2026-05-19 10:30+02', NULL, NULL, 'rm1', '2026-05-19 10:29+02', NULL, NULL, NULL);

-- ---------------------------------------------------------------- proposals
INSERT INTO proposal (id, portfolio_id, title, rationale, status, created_by, created_at, updated_at, sent_at,
                      decided_at, decided_by, reject_reason, signature_png) VALUES
 (1, 1, 'Add short-dated government bonds',
     'Lock in current yields and lower overall volatility ahead of year end, in line with the balanced profile.',
     'WORKING', 'rm1', '2026-10-15 09:00+02', '2026-10-15 09:20+02', NULL, NULL, NULL, NULL, NULL),
 (2, 6, 'Invest surplus cash in global equities',
     'About 85% of the portfolio is in cash. A phased move into diversified ETFs fits the balanced profile.',
     'SENT_FOR_VALIDATION', 'rm1', '2026-10-08 10:00+02', '2026-10-09 11:00+02', '2026-10-09 11:00+02', NULL, NULL, NULL, NULL),
 (3, 4, 'Increase income holdings',
     'Shift part of the cash into an income-oriented equity fund.',
     'EXECUTED', 'rm1', '2026-05-10 09:00+02', '2026-05-19 10:29+02', '2026-05-12 09:00+02', '2026-05-15 17:00+02', 'rm1', NULL, NULL);

INSERT INTO proposal_line (proposal_id, line_no, instrument_id, side, quantity, comment) VALUES
 (1, 1, 16, 'BUY', 20000, 'French government 2031'),
 (1, 2, 12, 'BUY', 80,    'Euro government bond ETF'),
 (2, 1, 10, 'BUY', 400,   'Core world equities'),
 (2, 2, 11, 'BUY', 250,   'All-world, adds emerging markets'),
 (2, 3, 12, 'BUY', 150,   'Government bonds for balance'),
 (3, 1, 5,  'BUY', 100,   NULL);

UPDATE trade_order SET proposal_id = 3 WHERE id = 7;

-- ---------------------------------------------------------------- messages
INSERT INTO message (client_id, from_client, sender, sender_name, body, created_at, read_at) VALUES
 (1, FALSE, 'rm1',     'Ravi Menon',   'Hello Clara, I have prepared a proposal to add some short-dated government bonds. Shall we go through it on Thursday?', '2026-10-15 09:25+02', '2026-10-15 12:10+02'),
 (1, TRUE,  'client1', 'Clara Dupont', 'Thursday at 11 works for me. Could you also send my September statement?', '2026-10-15 12:12+02', '2026-10-15 13:00+02'),
 (1, FALSE, 'rm1',     'Ravi Menon',   'Perfect, Thursday at 11. You can generate the statement yourself under Reports on your portfolio page.', '2026-10-15 13:02+02', NULL),
 (5, TRUE,  'amelie',  'Amélie Laurent', 'I received the proposal about investing my cash. Can we do it in two steps instead of one?', '2026-10-17 18:40+02', NULL);

-- Let new rows created through the app continue after the demo ids.
SELECT setval(pg_get_serial_sequence('instrument', 'id'),    (SELECT max(id) FROM instrument));
SELECT setval(pg_get_serial_sequence('client', 'id'),        (SELECT max(id) FROM client));
SELECT setval(pg_get_serial_sequence('portfolio', 'id'),     (SELECT max(id) FROM portfolio));
SELECT setval(pg_get_serial_sequence('cash_account', 'id'),  (SELECT max(id) FROM cash_account));
SELECT setval(pg_get_serial_sequence('position', 'id'),      (SELECT max(id) FROM position));
SELECT setval(pg_get_serial_sequence('mifid_profile', 'id'), (SELECT max(id) FROM mifid_profile));
SELECT setval(pg_get_serial_sequence('trade_order', 'id'),   (SELECT max(id) FROM trade_order));
SELECT setval(pg_get_serial_sequence('proposal', 'id'),      (SELECT max(id) FROM proposal));
SELECT setval(pg_get_serial_sequence('proposal_line', 'id'), (SELECT max(id) FROM proposal_line));
SELECT setval(pg_get_serial_sequence('message', 'id'),       (SELECT max(id) FROM message));
