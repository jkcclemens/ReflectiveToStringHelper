# ReflectiveToStringHelper

You may have heard of ToStringHelper, the awesome tool in Google's Guava project (MoreObjects#toStringHelper). This isn't that.

It's pretty similar, though.

ReflectiveToStringHelper (or just RTSH – arr tush) uses Java's reflection capabilities to pick up the fields of any object and generate a toString from them, whether they're private, package-local, protected, or public. Access to the fields doesn't matter.

Rather than drone on about how it works, take a look at some examples!

## Examples

```java
public class Person {

    public String firstName;
    public String lastName;
    public float age;
    public final float height = 6.083F; // Joe is done growing. He's 6'1".
    protected int numberOfFriends = 3;
    protected int numberOfExes = 2;
    private boolean inARelationship = false;
    private Person partner = null;
    private boolean thinksHeIsGreat = true;
    private volatile int timesCried = 100;

    public Person(final String firstName, final String lastName, final float age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    public void updateRelationshipStatus() {
        this.inARelationship = this.partner != null;
    }

    // As you'll see, we only work with fields. This never shows up anywhere.
    public String getLastSaidSentence() {
        return "Howdy!";
    }

    @Override
    public String toString() {
        return ReflectiveToStringHelper.of(this).generate();
    }
}

final Person joe = new Person("Joe", "Schmoe", 23.4F);
// 1. Let's only print what someone in the public would know about Joe.
// 1. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe
    )
);
// 2. How about his friends?
// 2. Person{firstName=Joe,lastName=Schmoe,numberOfFriends=3,numberOfExes=2,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).protecteds(true)
    )
);
// 3. The things only Joe knows
// 3. Person{thinksHeIsGreat=true,timesCried=100,partner=null,inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true)
    )
);
// 4. What someone in the public knows and ONLY his amount of friends
// 4. Person{firstName=Joe,lastName=Schmoe,numberOfFriends=3,age=23.4,height=6.083
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).ensure("numberOfFriends")
    )
);
// 5. Joe wants people to think he's in a relationship
// 5. Person{firstName=Joe,lastName=Schmoe,inARelationship=true,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).map("thinksHeIsGreat", "inARelationship").ensure("thinksHeIsGreat")
    )
);
// 6. Joe is a really open guy, except about his exes.
// 6. Person{thinksHeIsGreat=true,firstName=Joe,lastName=Schmoe,timesCried=100,partner=null,inARelationship=false,numberOfFriends=3,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().allVisibilities(true).exclude("numberOfExes")
    )
);
// 7. Joe isn't really a numbers guy with his family.
// 7. Person{firstName=Joe,lastName=Schmoe}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).protecteds(true).exclude(Integer.TYPE).exclude(Float.TYPE)
    )
);
// 8. Joe wants everyone to know the private on/off parts of his life, but nothing else.
// 8. Person{thinksHeIsGreat=true,inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure(Boolean.TYPE)
    )
);
// 9. The only number Joe really likes is his height.
// 9. Person{height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure("height", Float.TYPE)
    )
);
// 10. Joe doesn't want people to know the fixed parts of his life.
// 10. Person{firstName=Joe,lastName=Schmoe,age=23.4}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).keepFinals(false)
    )
);
// 11. Joe just can't control what makes him cry. He doesn't want people to know.
// 11. Person{thinksHeIsGreat=true,partner=null,inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true).keepVolatiles(false)
    )
);
// 12. Joe doesn't want to appear arrogant or weak!
// 12. Person{partner=null,inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true).exclude("thinksHeIsGreat").exclude("timesCried")
    )
);
// 13. Joe wants to share all of the positive aspects of his life.
// 13. Person{thinksHeIsGreat=true}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensureValue(true)
    )
);
// 14. When Joe is single, he's proud of it. He will only share his relationship status if he's single.
// 14. Person{inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure("inARelationship", Boolean.TYPE, false)
    )
);
// 15. Joe likes to alphabetize his attributes.
// 15. Person{thinksHeIsGreat=true,firstName=Joe,lastName=Schmoe,timesCried=100,partner=null,inARelationship=false,numberOfFriends=3,numberOfExes=2,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().allVisibilities(true)
    ).fieldComparator((f1, f2) -> f1.getName().compareTo(f2.getName()))
);
// 16. Joe thinks the empty parts of his life don't need sharing.
// 16. Person{thinksHeIsGreat=true,firstName=Joe,lastName=Schmoe,timesCried=100,inARelationship=false,numberOfFriends=3,numberOfExes=2,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().allVisibilities(true).omitNullValues(true)
    )
);
// 17. Looks like Joe has gone and got himself a girlfriend! He wants everyone to know.
// 17. Person{firstName=Joe,lastName=Schmoe,partner=Person{firstName=Jane,lastName=Doe,age=22.3,height=6.083},inARelationship=true,age=23.4,height=6.083}
joe.partner = new Person("Jane", "Doe", 22.3F);
joe.updateRelationshipStatus();
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).ensure("partner", Person.class).ensure("inARelationship")
    )
);
// 18. Note that another way to do the same as 17 would be to remove the inARelationship field and
//     updateRelationshipStatus() method, then use a custom to display a fake field in the toString.
// 18. Person{firstName=Joe,lastName=Schmoe,partner=Person{firstName=Jane,lastName=Doe,age=22.3,height=6.083},inARelationship=true,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).ensure("partner", Person.class).custom("inARelationship", joe.partner != null)
    )
);
// 19. Alphabetizing custom fields with declared fields.
// 19. Person{age=23.4,firstName=Joe,height=6.083,inARelationship=true,lastName=Schmoe,partner=Person{firstName=Jane,lastName=Doe,age=22.3,height=6.083}}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).ensure("partner", Person.class).custom("inARelationship", joe.partner != null)
    ).entryComparator((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
);
// 20. What about a missing person?
// 20. null
log(
    ReflectiveToStringHelper.of(
        null
    )
);
// 21. Joe's hash code is interesting.
// 21. Person@66a29884{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe
    ).hashCode(true)
);
// 22. Custom symbols!
// 22. Person#66a29884{firstName is Joe; lastName is Schmoe; age is 23.4; height is 6.083}
log(
    ReflectiveToStringHelper.of(
        joe
    ).hashCode(true).hashCodeSymbol("#").separator("; ").equality(" is ")
);
```
