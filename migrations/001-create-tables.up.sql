CREATE TABLE IF NOT EXISTS rtrac.watch_subreddit (
			 id serial PRIMARY KEY,
			 subreddit VARCHAR (128) NOT NULL,
			 keywords VARCHAR (512) NOT NULL,
			 ignore_keywords VARCHAR (512),
			 ignore_domain VARCHAR (128),
			 check_flair BOOLEAN DEFAULT false,
			 active BOOLEAN DEFAULT false,
			 email VARCHAR (512) NOT NULL CHECK (email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$'),
			 created_on TIMESTAMPTZ DEFAULT now()
);
--;;
