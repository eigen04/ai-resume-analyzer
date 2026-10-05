DROP INDEX IF EXISTS vector_store_embedding_idx;
ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(768);
CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
