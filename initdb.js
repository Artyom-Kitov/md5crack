db = db.getSiblingDB('admin');
db.auth('md5crack', 'goga123');

if (!rs.status().ok) {
  rs.initiate({
    _id: "rs0",
    members: [
      { _id: 0, host: "md5-mongo-primary:27017", priority: 2 },
      { _id: 1, host: "md5-mongo-secondary1:27017", priority: 1 },
      { _id: 2, host: "md5-mongo-secondary2:27017", priority: 1 }
    ]
  });
}