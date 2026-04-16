UPDATE courses SET max_capacity = 5
WHERE max_capacity = 0 OR max_capacity IS NULL;
