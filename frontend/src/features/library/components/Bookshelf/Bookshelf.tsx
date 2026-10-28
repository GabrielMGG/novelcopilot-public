import './Bookshelf.css';
import type { ShelfItem, StoryBook } from "../../types/library.types";
import { AddBookCard } from "../AddBookCard/AddBookCard";
import { BookCard } from "../BookCard/BookCard";

type BookshelfProps = {
  shelves: ShelfItem[][];
  onCreateStory: () => void;
  onEditStory: (book: StoryBook) => void;
  onMoveStoryToTrash: (book: StoryBook) => void;
};

export function Bookshelf({ shelves, onCreateStory, onEditStory, onMoveStoryToTrash }: BookshelfProps) {
  return (
    <div className="shelves">
      {shelves.map((row, shelfIndex) => (
        <div className="shelf" key={shelfIndex}>
          <div className="shelf-books">
            {row.map((item, itemIndex) =>
              "isAdd" in item ? (
                <AddBookCard key={`add-${itemIndex}`} onClick={onCreateStory} />
              ) : (
                <BookCard 
                  book={item} 
                  key={item.id} 
                  onEdit={onEditStory}
                  onMoveToTrash={onMoveStoryToTrash}
                />
              )
            )}
          </div>

          <div className="shelf-plank" />
        </div>
      ))}
    </div>
  );
}

