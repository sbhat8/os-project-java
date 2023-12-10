![Imgur](https://imgur.com/z4a1Nic.jpg)
# Multi-threaded Web Crawler 
*🕷️ Welcome to the core of our OS-Project! Our multi-threaded web crawler is meticulously designed for optimal efficiency, dedicated to the seamless extraction of data from The Weather Network – your ultimate destination for weather-related insights. 🌦️*

## Description

Currently, the crawler is able to take a query as parameter and search https://amazon.com for search results and product details. The crawler is able to extract the following information from the search results and product details pages:

- Product title

- Product link

- Product price

- Product rating

## Requirements

Beyond the requirements in `requirements.txt`, you will need to install RabbitMQ as a message broker.

 🐇 **How to get RabbitMQ on your side:**
  1. Visit [RabbitMQ Download](https://www.rabbitmq.com/download.html)
  2. Follow the simple installation steps.

## Key commands

### To run the development server

```

pip install -r requirements.txt

```

```

uvicorn main:app --reload

```

Then, go to http://localhost:8000/docs to view the API documentation and test the API.

### To start celery workers

```

celery -A app.tasks worker --loglevel=info --concurrency=2 -E -P eventlet

```

This starts 2 workers. The number of workers can be changed by changing the `--concurrency` flag.

### To start flower monitoring

```

celery --broker=amqp://guest:guest@localhost:5672// flower

```

Then, go to http://localhost:5555 to view the flower dashboard.

### Run the crawler

🏃‍♀️ Ready to set the crawler in motion? Follow these simple steps:

1.  🚀 Head to [http://localhost:8000/docs](http://localhost:8000/docs).
2.  🎯 Click on the `/async/scrape/amazon/{query}` endpoint.
3.  🧐 Hit the `Try it out` button.
4.  📝 Enter your query in the `query` field.
5.  🚀 Click `Execute` to unleash the crawler.

🌼 As the crawler runs, you can go to http://localhost:5555 to view the flower dashboard and monitor the progress of the crawler and see results.

👷‍♂️ If you would like to run the crawler without using celery workers (without concurrency), you can use the `/scrape/amazon/{query}` endpoint instead.

Adventure awaits! Let the crawling commence! 🌐✨
