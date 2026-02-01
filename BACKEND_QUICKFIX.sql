-- IMMEDIATE FIX: Update users table with required fields
-- Run this SQL in your backend database

-- Option 1: Update specific user (healthworker1)
UPDATE users 
SET phcName = 'Primary Health Center - Chennai North',
    hubName = 'Zone 3 Hub',
    blockName = 'Teynampet Block',
    districtName = 'Chennai'
WHERE username = 'healthworker1';

-- Option 2: Update all test users at once
UPDATE users 
SET phcName = CASE username
        WHEN 'healthworker1' THEN 'Primary Health Center - Chennai North'
        WHEN 'labtech1' THEN 'District Hospital - Coimbatore'
        WHEN 'admin1' THEN 'Directorate of Public Health'
        ELSE phcName
    END,
    hubName = CASE username
        WHEN 'healthworker1' THEN 'Zone 3 Hub'
        WHEN 'labtech1' THEN 'Zone 5 Hub'
        WHEN 'admin1' THEN 'Central Hub'
        ELSE hubName
    END,
    blockName = CASE username
        WHEN 'healthworker1' THEN 'Teynampet Block'
        WHEN 'labtech1' THEN 'RS Puram Block'
        WHEN 'admin1' THEN 'HQ Block'
        ELSE blockName
    END,
    districtName = CASE username
        WHEN 'healthworker1' THEN 'Chennai'
        WHEN 'labtech1' THEN 'Coimbatore'
        WHEN 'admin1' THEN 'Chennai'
        ELSE districtName
    END
WHERE username IN ('healthworker1', 'labtech1', 'admin1');

-- Verify the fix
SELECT id, username, name, phcName, hubName, blockName, districtName 
FROM users 
WHERE username IN ('healthworker1', 'labtech1', 'admin1');

-- If columns don't exist, add them first:
-- ALTER TABLE users ADD COLUMN phcName VARCHAR(255);
-- ALTER TABLE users ADD COLUMN hubName VARCHAR(255);
-- ALTER TABLE users ADD COLUMN blockName VARCHAR(255);
-- ALTER TABLE users ADD COLUMN districtName VARCHAR(255);
