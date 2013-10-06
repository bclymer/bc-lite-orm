bc-lite-orm
===========

This is a very lightweight (21kb 
for now) ORM for Android. Use this when you want to keep your app size down and just want to store peristant data without all the features of a full ORM.

How to Use
===========
You will need a class that extends BcSQLiteOpenHelper
```
public class DatabaseHelper extends BcSQLiteOpenHelper {
  
	public DatabaseHelper(Context context) {
		super(context, "Test.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		TableUtil.createTable(db, Person.class); // Creates the table
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}
```

The Person class looks like this
```
@PersistantObject
public class Person {
  @PersistantField(columnName = "person_name") // optional
	public String name;
	@PersistantField(id = true) // could also be generatedId
	public int age;
}
```

Creating a Dao would go like this:
`BcDao<Person, Integer> bcDao = new BcDao<Person, Integer>(Person.class);`

Then you could query the table with
`List<Person> people = bcDao.queryBuilder().lt("age", "" + 20).orderBy("age", false).limit(5).execute();`

It also has the basic methods of:
insert
insertMany
update
updateMany
delete
deleteMany
