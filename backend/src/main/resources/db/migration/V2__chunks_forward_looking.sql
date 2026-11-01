-- V2: store the cleaner's forward-looking tag on each chunk (roadmap E1.5 / E1.8).
-- Forward-looking-statement boilerplate is kept but tagged, so evaluation can later
-- decide whether to exclude it from retrieval (spec 7.2 step 3).
ALTER TABLE chunks ADD COLUMN forward_looking BOOLEAN NOT NULL DEFAULT false;
