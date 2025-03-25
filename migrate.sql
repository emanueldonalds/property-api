RENAME TABLE price_change TO price_change_old;
ALTER TABLE price_change_old DROP FOREIGN KEY FK2arlfkneupik3lg49j0j2jtqw;
RENAME TABLE listing TO listing_old;
RENAME TABLE listing_visibility TO listing_visibility_old;
ALTER TABLE listing_visibility_old DROP FOREIGN KEY FKk9ijq3u5wawuhwe7ddd359h27;

ALTER TABLE price_change_old ADD COLUMN effective_from TIMESTAMP NULL, ADD COLUMN effective_to TIMESTAMP NULL;

BEGIN;

-- Resolve effective dates on exiting price changes
-- This leaves effective_from null if there is only one price change
UPDATE price_change_old pc1
SET effective_to = pc1.last_seen,
effective_from = (
  SELECT MAX(pc2.last_seen)
  FROM price_change_old pc2
  WHERE pc2.listing_id = pc1.listing_id
    AND pc2.last_seen < pc1.last_seen
);

-- Set effective_from to listing.first_seen where they are null
UPDATE 
    price_change_old 
INNER JOIN 
    listing_old 
ON 
    price_change_old.listing_id = listing_old.id 
SET 
    effective_from = listing_old.first_seen 
WHERE 
    effective_from IS NULL;


-- Insert the current price for listings with change history
INSERT INTO price_change_old (
    listing_id,
    price,
    effective_from
)
SELECT 
    listing_old.id, 
    listing_old.price, 
    MAX(price_change_old.effective_to) 
FROM 
    listing_old 
INNER JOIN 
    price_change_old 
ON 
    price_change_old.listing_id = listing_old.id 
GROUP BY 
    price_change_old.listing_id;


-- Insert those without any price change entry
INSERT INTO price_change_old (
    listing_id,
    price,
    effective_from
)
SELECT 
    listing_old.id,
    listing_old.price,
    listing_old.first_seen
FROM listing_old 
LEFT JOIN price_change_old 
ON price_change_old.listing_id = listing_old.id 
WHERE price_change_old.id IS NULL;

--At this point start property-api and let it create the new tables

INSERT INTO listing (
    id,
    address,
    agency,
    build_year,
    deleted,
    price,
    first_seen,
    last_seen,
    last_updated,
    monthly_charge,
    name,
    rooms,
    size_name,
    size_value,
    url
)
SELECT
    id,
    address,
    agency,
    build_year,
    deleted,
    price,
    first_seen,
    last_seen,
    last_updated,
    monthly_charge,
    name,
    rooms,
    size_name,
    size_value,
    url
FROM listing_old;

INSERT INTO price_change (
    id,
    effective_from,
    effective_to,
    price,
    listing_id
)
SELECT
    id,
    effective_from,
    effective_to,
    price,
    listing_id
FROM price_change_old;

COMMIT;

DROP TABLE listing_visibility_old;
DROP TABLE price_change_old;
DROP TABLE listing_old;
